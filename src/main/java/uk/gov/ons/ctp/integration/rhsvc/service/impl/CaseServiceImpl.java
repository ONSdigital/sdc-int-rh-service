package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static java.util.stream.Collectors.toList;
import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Product;
import uk.gov.ons.ctp.common.domain.ProductGroup;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.FulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PrintFulfilmentRequestDTO;

/** Implementation to deal with Case data */
@Slf4j
@Service
public class CaseServiceImpl {
  @Autowired private AppConfig appConfig;
  @Autowired private CaseRepository dataRepo;
  @Autowired private SurveyRepository surveyRepository;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private RateLimiterClient rateLimiterClient;

  private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public List<CaseDTO> findCasesBySampleAttribute(
      final String attributeKey, final String attributeValue) throws CTPException {

    List<CaseUpdate> foundCases =
        dataRepo.findCaseUpdatesBySampleAttribute(attributeKey, attributeValue, true);
    log.debug(
        "Search for cases by attribute value",
        kv("numberFoundCase", foundCases.size()),
        kv("searchAttributeName", attributeKey),
        kv("searchValue", attributeValue));
    return mapperFacade.mapAsList(foundCases, CaseDTO.class);
  }

  //  /**
  //   * This method contains the business logic for submitting a fulfilment by SMS request.
  //   *
  //   * @param requestBodyDTO contains the parameters from the originating http POST request.
  //   * @throws CTPException if the specified case cannot be found, or if no matching product is
  // found.
  //   */
  //  public void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException
  // {
  //    Contact contact = new Contact();
  //    contact.setTelNo(requestBodyDTO.getTelNo());
  //    CaseUpdate caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
  //    var products = createProductList(DeliveryChannel.SMS, requestBodyDTO, caseDetails);
  //    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
  //    createAndSendFulfilments(DeliveryChannel.SMS, contact, requestBodyDTO, products);
  //  }

  public void fulfilmentRequestByPost(PrintFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    Contact contact = new Contact();
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());
    CaseUpdate caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
    SurveyUpdate surveyUpdate = surveyRepository.readSurvey(caseDetails.getSurveyId()).get();
    var products = createProductList(DeliveryChannel.POST, requestBodyDTO, surveyUpdate);
    validateContactName(contact);
    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
    createAndSendFulfilments(DeliveryChannel.POST, requestBodyDTO, contact, products);
  }

  public void sendNewCaseEvent(NewCaseDTO newCaseDTO) throws CTPException {
    log.debug(
        "Entering createAndSendNewCase",
        kv("schoolId", newCaseDTO.getSchoolId()),
        kv("lastName", newCaseDTO.getLastName()));

    // Use the fixed collexid when sending the newCase event
    UUID collectionExerciseId = appConfig.getSis().getCollectionExerciseIdAsUUID();

    NewCasePayloadContent payload = createNewCaseRequestPayload(newCaseDTO, collectionExerciseId);

    eventPublisher.sendEvent(TopicType.NEW_CASE, Source.RESPONDENT_HOME, Channel.RH, payload);
  }

  private NewCasePayloadContent createNewCaseRequestPayload(
      NewCaseDTO caseRegistrationDTO, UUID collectionExerciseId) throws CTPException {

    final UUID caseId = UUID.randomUUID();

    Map<String, String> sample = new HashMap<>();
    sample.put(NewCasePayloadContent.ATTRIBUTE_SCHOOL_ID, caseRegistrationDTO.getSchoolId());
    sample.put(NewCasePayloadContent.ATTRIBUTE_SCHOOL_NAME, caseRegistrationDTO.getSchoolName());
    sample.put(
        NewCasePayloadContent.ATTRIBUTE_CONSENT_GIVEN_TEST,
        Boolean.toString(caseRegistrationDTO.isConsentGivenTest()));
    sample.put(
        NewCasePayloadContent.ATTRIBUTE_CONSENT_GIVEN_SURVEY,
        Boolean.toString(caseRegistrationDTO.isConsentGivenSurvey()));

    Map<String, String> sampleSensitive = new HashMap<>();
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_FIRST_NAME, caseRegistrationDTO.getFirstName());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_LAST_NAME, caseRegistrationDTO.getLastName());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_CHILD_FIRST_NAME, caseRegistrationDTO.getChildFirstName());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_CHILD_MIDDLE_NAMES,
        caseRegistrationDTO.getChildMiddleNames());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_CHILD_LAST_NAME, caseRegistrationDTO.getChildLastName());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_CHILD_DOB,
        caseRegistrationDTO.getChildDob().format(DOB_FORMATTER));
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_MOBILE_NUMBER, caseRegistrationDTO.getMobileNumber());
    sampleSensitive.put(
        NewCasePayloadContent.ATTRIBUTE_EMAIL_ADDRESS, caseRegistrationDTO.getEmailAddress());

    return NewCasePayloadContent.builder()
        .caseId(caseId)
        .collectionExerciseId(collectionExerciseId)
        .sample(sample)
        .sampleSensitive(sampleSensitive)
        .build();
  }

  /*
   * create a cached list of product information to use for both
   * rate-limiting and event generation.
   * this prevents multiple calls to repeat getting products details.
   * NOTE: must return list in order of fulfilmentCodes
   */
  private List<Product> createProductList(
      DeliveryChannel deliveryChannel, FulfilmentRequestDTO request, SurveyUpdate surveyUpdate)
      throws CTPException {
    Map<String, Product> map = new HashMap<>();
    for (String fulfilmentCode : new HashSet<>(request.getFulfilmentCodes())) {
      SurveyFulfilment fulfilment =
          findSurveyFulfilment(fulfilmentCode, deliveryChannel, surveyUpdate);
      Product product = new Product();
      product.setProductGroup(ProductGroup.UAC);
      product.setDeliveryChannel(deliveryChannel);
      product.setFulfilmentCode(fulfilmentCode);
      product.setDescription(fulfilment.getDescription());

      String[] languageStrings =
          fulfilment
              .getMetadata()
              .get("languages")
              .toString()
              .replace("[", "")
              .replace("]", "")
              .split(",");
      List<Language> languages = new ArrayList<>();
      for (String languageString : languageStrings) {
        languageString = languageString.replace("\"", "").trim();
        languages.add(Language.lookup(languageString));
      }
      product.setLanguages(languages);

      map.put(fulfilmentCode, product);
    }
    return request.getFulfilmentCodes().stream().map(fc -> map.get(fc)).collect(toList());
  }

  private void recordRateLimiting(
      Contact contact, String ipAddress, List<Product> products, CaseUpdate caseDetails)
      throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      for (Product product : products) {
        log.debug("Recording rate-limiting", kv("fulfilmentCode", product.getFulfilmentCode()));
        UniquePropertyReferenceNumber uprn =
            UniquePropertyReferenceNumber.create(
                caseDetails.getSample().get(CaseUpdate.ATTRIBUTE_UPRN));
        recordRateLimiting(contact, product, ipAddress, uprn);
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
      Contact contact, Product product, String ipAddress, UniquePropertyReferenceNumber uprn)
      throws CTPException {

    rateLimiterClient.checkFulfilmentRateLimit(
        Domain.RH, product, ipAddress, uprn, contact.getTelNo());
  }

  private void createAndSendFulfilments(
      DeliveryChannel deliveryChannel,
      FulfilmentRequestDTO request,
      Contact contact,
      List<Product> products) {
    log.debug(
        "Entering createAndSendFulfilment",
        kv("fulfilmentCodes", request.getFulfilmentCodes()),
        kv("deliveryChannel", deliveryChannel));

    for (Product product : products) {
      FulfilmentRequest payload =
          createFulfilmentRequestPayload(request.getCaseId(), contact, product);

      eventPublisher.sendEvent(TopicType.FULFILMENT, Source.RESPONDENT_HOME, Channel.RH, payload);
    }
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      UUID caseId, Contact contact, Product product) {

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setPackCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());

    Map<String, Object> personalisation = new HashMap<>();
    if (Strings.isNotBlank(contact.getForename())) {
      personalisation.put("firstName", contact.getForename());
    }
    if (Strings.isNotBlank(contact.getSurname())) {
      personalisation.put("lastName", contact.getSurname());
    }
    fulfilmentRequest.setPersonalisation(personalisation);

    return fulfilmentRequest;
  }

  private SurveyFulfilment findSurveyFulfilment(
      String packCode, DeliveryChannel deliveryChannel, SurveyUpdate surveyUpdate)
      throws CTPException {
    log.debug(
        "Attempting to find surveyFulfilment.",
        kv("deliveryChannel", deliveryChannel),
        kv("packCode", packCode));

    List<SurveyFulfilment> fulfilments;

    switch (deliveryChannel) {
      case POST -> fulfilments = surveyUpdate.getAllowedPrintFulfilments();
      case SMS -> fulfilments = surveyUpdate.getAllowedSmsFulfilments();
      case EMAIL -> fulfilments = surveyUpdate.getAllowedEmailFulfilments();
      default -> throw new CTPException(Fault.SYSTEM_ERROR, "Unknown delivery channel");
    }

    for (SurveyFulfilment surveyFulfilment : fulfilments) {
      if (surveyFulfilment.getPackCode().equals(packCode)) {
        return surveyFulfilment;
      }
    }

    log.warn("Fulfilment not compatible with survey", kv("fulfilmentCode", packCode));
    throw new CTPException(Fault.BAD_REQUEST, "Fulfilment not compatible with survey");
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

  private CaseUpdate findCaseDetails(UUID caseId) throws CTPException {
    return dataRepo
        .readCaseUpdate(caseId.toString())
        .orElseThrow(
            () -> {
              log.info("Case not found", kv("caseId", caseId));
              return new CTPException(Fault.RESOURCE_NOT_FOUND, "Case not found: " + caseId);
            });
  }
}
