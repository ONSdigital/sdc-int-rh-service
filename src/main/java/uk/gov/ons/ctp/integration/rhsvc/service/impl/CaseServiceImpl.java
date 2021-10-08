package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static java.util.stream.Collectors.toList;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.NewCaseSample;
import uk.gov.ons.ctp.common.event.model.NewCaseSampleSensitive;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.FulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Implementation to deal with Case data */
@Slf4j
@Service
public class CaseServiceImpl implements CaseService {
  @Autowired private AppConfig appConfig;
  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private ProductReference productReference;
  @Autowired private RateLimiterClient rateLimiterClient;

  private MapperFacade mapper = new RHSvcBeanMapper();

  @Override
  public CaseDTO getLatestValidNonHICaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {

    Optional<CollectionCase> caseFound =
        dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(uprn.getValue()), true);
    if (caseFound.isPresent()) {
      log.debug(
          "non HI latest valid case retrieved for UPRN",
          kv("case", caseFound.get().getId()),
          kv("uprn", uprn));
      return mapperFacade.map(caseFound.get(), CaseDTO.class);
    } else {
      log.warn("No cases returned for uprn", kv("uprn", uprn));
      throw new CTPException(Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case");
    }
  }

  /**
   * This method contains the business logic for submitting a fulfilment by SMS request.
   *
   * @param requestBodyDTO contains the parameters from the originating http POST request.
   * @throws CTPException if the specified case cannot be found, or if no matching product is found.
   */
  @Override
  public void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());
    CollectionCase caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
    var products = createProductList(DeliveryChannel.SMS, requestBodyDTO, caseDetails);
    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
    createAndSendFulfilments(DeliveryChannel.SMS, contact, requestBodyDTO, products, caseDetails);
  }

  @Override
  public void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());
    CollectionCase caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
    var products = createProductList(DeliveryChannel.POST, requestBodyDTO, caseDetails);
    preValidatePostalContactDetails(products, contact);
    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
    createAndSendFulfilments(DeliveryChannel.POST, contact, requestBodyDTO, products, caseDetails);
  }

  @Override
  public void sendNewCaseEvent(NewCaseDTO newCaseDTO) throws CTPException {
    log.debug(
        "Entering createAndSendNewCase",
        kv("collectionExerciseSid", newCaseDTO.getCollectionExerciseId()),
        kv("schoolId", newCaseDTO.getSchoolId()),
        kv("lastName", newCaseDTO.getLastName()));

    NewCasePayloadContent payload = createNewCaseRequestPayload(newCaseDTO);

    eventPublisher.sendEvent(TopicType.NEW_CASE, Source.RESPONDENT_HOME, Channel.RH, payload);
  }

  private NewCasePayloadContent createNewCaseRequestPayload(NewCaseDTO caseRegistrationDTO)
      throws CTPException {

    final UUID caseId = UUID.randomUUID();
    final UUID collectionExerciseId = caseRegistrationDTO.getCollectionExerciseId();

    NewCaseSample newCaseSample = mapper.map(caseRegistrationDTO, NewCaseSample.class);
    NewCaseSampleSensitive newCaseSampleSensitive =
        mapper.map(caseRegistrationDTO, NewCaseSampleSensitive.class);

    return new NewCasePayloadContent(
        caseId, collectionExerciseId, newCaseSample, newCaseSampleSensitive);
  }

  /*
   * create a cached list of product information to use for both
   * rate-limiting and event generation.
   * this prevents multiple calls to repeat getting products details.
   * NOTE: must return list in order of fulfilmentCodes
   */
  private List<Product> createProductList(
      DeliveryChannel deliveryChannel, FulfilmentRequestDTO request, CollectionCase caseDetails)
      throws CTPException {
    Map<String, Product> map = new HashMap<>();
    Region region = Region.valueOf(caseDetails.getAddress().getRegion());
    for (String fulfilmentCode : new HashSet<>(request.getFulfilmentCodes())) {
      map.put(fulfilmentCode, findProduct(fulfilmentCode, deliveryChannel, region));
    }
    return request.getFulfilmentCodes().stream().map(fc -> map.get(fc)).collect(toList());
  }

  private void preValidatePostalContactDetails(List<Product> products, Contact contact)
      throws CTPException {
    for (Product product : products) {
      if (isIndividual(product)) {
        validateContactName(contact);
      }
    }
  }

  private void recordRateLimiting(
      Contact contact, String ipAddress, List<Product> products, CollectionCase caseDetails)
      throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      for (Product product : products) {
        log.debug("Recording rate-limiting", kv("fulfilmentCode", product.getFulfilmentCode()));
        CaseType caseType = CaseType.valueOf(caseDetails.getCaseType());
        UniquePropertyReferenceNumber uprn =
            UniquePropertyReferenceNumber.create(caseDetails.getAddress().getUprn());
        recordRateLimiting(contact, product, caseType, ipAddress, uprn);
      }
    } else {
      log.info("Rate limiter client is disabled");
    }
  }

  /*
   * Call the rate limiter. The RateLimiterClient invokes the EnvoyLimiter within a circuit-breaker,
   * thus protecting the RHSvc in the unlikely event that the rate limiter service is failing.
   *
   * If the limit is breached a ResponseStatusException with HTTP 429 will be thrown.
   * If Rate limiter validation fails then a CTPException is thrown.
   */
  private void recordRateLimiting(
      Contact contact,
      Product product,
      CaseType caseType,
      String ipAddress,
      UniquePropertyReferenceNumber uprn)
      throws CTPException {

    rateLimiterClient.checkFulfilmentRateLimit(
        Domain.RH, product, caseType, ipAddress, uprn, contact.getTelNo());
  }

  private void createAndSendFulfilments(
      DeliveryChannel deliveryChannel,
      Contact contact,
      FulfilmentRequestDTO request,
      List<Product> products,
      CollectionCase caseDetails)
      throws CTPException {
    log.debug(
        "Entering createAndSendFulfilment",
        kv("fulfilmentCodes", request.getFulfilmentCodes()),
        kv("deliveryChannel", deliveryChannel));

    for (Product product : products) {
      FulfilmentRequest payload =
          createFulfilmentRequestPayload(request.getCaseId(), contact, product, caseDetails);

      eventPublisher.sendEvent(TopicType.FULFILMENT, Source.RESPONDENT_HOME, Channel.RH, payload);
    }
  }

  private boolean isIndividual(Product product) {
    return product.getIndividual() == null ? false : product.getIndividual();
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      UUID caseId, Contact contact, Product product, CollectionCase caseDetails)
      throws CTPException {

    String individualCaseId = null;
    if (isIndividual(product) && CaseType.HH.name().equals(caseDetails.getCaseType())) {
      individualCaseId = UUID.randomUUID().toString();
    }

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setIndividualCaseId(individualCaseId);
    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);
    return fulfilmentRequest;
  }

  private Product findProduct(String fulfilmentCode, DeliveryChannel deliveryChannel, Region region)
      throws CTPException {
    log.debug(
        "Attempting to find product.",
        kv("region", region),
        kv("deliveryChannel", deliveryChannel),
        kv("fulfilmentCode", fulfilmentCode));

    // Build search criteria base on the cases details and the requested fulfilmentCode
    Product searchCriteria = new Product();
    searchCriteria.setRequestChannels(Collections.singletonList(RequestChannel.RH));
    searchCriteria.setRegions(Collections.singletonList(region));
    searchCriteria.setDeliveryChannel(deliveryChannel);
    searchCriteria.setFulfilmentCode(fulfilmentCode);

    // Attempt to find matching product
    return productReference.searchProducts(searchCriteria).stream()
        .findFirst()
        .orElseThrow(
            () -> {
              log.warn("Compatible product cannot be found", kv("searchCriteria", searchCriteria));
              return new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
            });
  }

  // Read case from firestore
  private CollectionCase findCaseDetails(UUID caseId) throws CTPException {
    return dataRepo
        .readCollectionCase(caseId.toString())
        .orElseThrow(
            () -> {
              log.info("Case not found", kv("caseId", caseId));
              return new CTPException(Fault.RESOURCE_NOT_FOUND, "Case not found: " + caseId);
            });
  }

  private void validateContactName(Contact contact) throws CTPException {
    if (StringUtils.isBlank(contact.getForename()) || StringUtils.isBlank(contact.getSurname())) {

      log.warn("Individual fields are required for the requested fulfilment");
      throw new CTPException(
          Fault.BAD_REQUEST,
          "The fulfilment is for an individual so none of the following fields can be empty: "
              + "'forename' and 'surname'");
    }
  }
}
