package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Product;
import uk.gov.ons.ctp.common.domain.ProductGroup;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.PrintFulfilmentRequestDTO;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(
    classes = {WebformServiceImpl.class, AppConfig.class, ValidationAutoConfiguration.class})
public class CaseServiceImplFulfilmentTest {

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private AppConfig appConfig;

  @Mock private CaseRepository dataRepo;

  @Mock private SurveyRepository surveyRepository;

  @Mock private EventPublisher eventPublisher;

  @Mock private RateLimiterClient rateLimiterClient;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Captor private ArgumentCaptor<Product> productCaptor;

  private CaseUpdate caseDetails;
  private SurveyUpdate surveyUpdate;
  // private SMSFulfilmentRequestDTO smsRequest;
  private PrintFulfilmentRequestDTO printRequest;
  private PrintFulfilmentRequestDTO badPrintRequest;
  private List<PrintFulfilmentRequestDTO> printFulfilmentRequestDTOS;
  private final Product p1 =
      new Product(
          "REPLACEMENT_UAC",
          "Replacement UAC Letter",
          ProductGroup.UAC,
          DeliveryChannel.POST,
          List.of(Language.ENGLISH));
  private final Product p2 =
      new Product(
          "DUMMY_FULFILMENT",
          "Dummy Fulfilment",
          ProductGroup.UAC,
          DeliveryChannel.POST,
          List.of(Language.ENGLISH, Language.WELSH));

  @BeforeEach
  public void setUp() throws Exception {
    this.caseDetails = FixtureHelper.loadPackageFixtures(CaseUpdate[].class).get(0);
    this.surveyUpdate = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    // this.smsRequest = FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);

    this.printFulfilmentRequestDTOS =
        FixtureHelper.loadClassFixtures(PrintFulfilmentRequestDTO[].class);
    this.printRequest = printFulfilmentRequestDTOS.get(0);
    this.badPrintRequest = printFulfilmentRequestDTOS.get(1);
    lenient().when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(true));
  }

  private RateLimiterConfig rateLimiterConfig(boolean enabled) {
    RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
    rateLimiterConfig.setEnabled(enabled);
    return rateLimiterConfig;
  }

  // --- fulfilment by SMS

  //  @Test
  //  public void shouldFulfilRequestBySmsForHousehold() throws Exception {
  //    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, false);
  //    // Individual case id field should not be set for non-individual
  //    assertNull(eventPayload.getIndividualCaseId());
  //  }
  //
  //  @Test
  //  public void shouldFulfilRequestBySmsForHouseholdWhenProductReturnsNullIndividual()
  //      throws Exception {
  //    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, null);
  //    assertNull(eventPayload.getIndividualCaseId());
  //  }
  //
  //  @Test
  //  public void shouldFulfilRequestBySmsForIndividual() throws Exception {
  //    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, true);
  //
  //    // Individual case id field should be populated as case+product is for an individual
  //    String individualUuid = eventPayload.getIndividualCaseId();
  //    assertNotNull(individualUuid);
  //    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  //  }
  //
  //  @Test
  //  public void shouldFulfilRequestBySmsForIndividualWhereProductHasMultipleCaseTypes()
  //      throws Exception {
  //    CaseUpdate caseDetails = caseUpdate.get(0);
  //    FulfilmentRequest eventPayload =
  //        doFulfilmentRequestBySMS(true, caseDetails, Product.CaseType.CE, Product.CaseType.HH);
  //
  //    // Individual case id field should be populated as case+product is for an individual
  //    String individualUuid = eventPayload.getIndividualCaseId();
  //    assertNotNull(individualUuid);
  //    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  //  }
  //
  //  private FulfilmentRequest doFulfilmentRequestBySMS(Product.CaseType caseType, Boolean
  // individual)
  //      throws Exception {
  //    CaseUpdate caseDetails = selectCaseUpdateForTest(caseType, individual);
  //    return doFulfilmentRequestBySMS(individual, caseDetails, caseType);
  //  }
  //
  //  private FulfilmentRequest doFulfilmentRequestBySMS(
  //      Boolean individual, CaseUpdate caseDetails, Product.CaseType... caseTypes) throws
  // Exception {
  //    String caseId = caseDetails.getCaseId();
  //    smsRequest.setCaseId(UUID.fromString(caseId));
  //    when(dataRepo.readCaseUpdate(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
  //    mockProductSearch("F1", individual, DeliveryChannel.SMS, caseTypes);
  //    caseSvc.fulfilmentRequestBySMS(smsRequest);
  //
  //    String phoneNo = "07714111222";
  //    verifyRateLimiterCall(1, phoneNo, smsRequest.getClientIP(), caseDetails);
  //    Contact contact = new Contact();
  //    contact.setTelNo(phoneNo);
  //    FulfilmentRequest eventPayload =
  //        getAndValidatePublishedEvent(caseDetails, contact, "F1").get(0);
  //    return eventPayload;
  //  }
  //
  //  @Test
  //  public void shouldRejectSmsFulfilmentForUnknownCase() throws Exception {
  //    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.empty());
  //    CTPException e =
  //        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
  //    assertTrue(e.getMessage().contains("Case not found"));
  //    verifyRateLimiterNotCalled();
  //  }
  //
  //  @Test
  //  public void shouldRejectSmsFulfilmentForUnknownProduct() throws Exception {
  //    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseUpdate.get(0)));
  //    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());
  //    CTPException e =
  //        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
  //    assertTrue(e.getMessage().contains("Compatible product cannot be found"));
  //    verifyRateLimiterNotCalled();
  //  }

  // --- fulfilment by post

  @Test
  public void shouldFulfilRequestByPost() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    caseSvc.fulfilmentRequestByPost(printRequest);

    verifyRateLimiterCall(1, null, printRequest.getClientIP(), caseDetails);
    Contact contact = new Contact();
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    getAndValidatePublishedEvent(caseDetails, contact, "REPLACEMENT_UAC").get(0);
  }

  private void assertRejectPostalFulfilmentForIndividualWithoutContactName() throws Exception {
    String caseId = caseDetails.getCaseId();
    printRequest.setCaseId(UUID.fromString(caseId));
    when(dataRepo.readCaseUpdate(eq(caseId))).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(printRequest));
    assertTrue(
        e.getMessage()
            .contains(
                "The fulfilment is for an individual so none of the following fields can be empty"));
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldRejectPostalFulfilmentWithoutForename() throws Exception {
    printRequest.setForename(null);
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentWithoutSurname() throws Exception {
    printRequest.setSurname(null);
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentWithEmptyForename() throws Exception {
    printRequest.setForename("");
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentWithEmptySurname() throws Exception {
    printRequest.setSurname("");
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownCase() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.empty());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(printRequest));
    assertTrue(e.getMessage().contains("Case not found"));
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownSurveyFulfilment() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(badPrintRequest));
    assertEquals("Fulfilment not compatible with survey", e.getMessage());
    verifyRateLimiterNotCalled();
  }

  // --- multi postal fulfilment tests

  @Test
  public void shouldFulfilRequestByPostForMultipleFulfilmentCodes() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    caseSvc.fulfilmentRequestByPost(printFulfilmentRequestDTOS.get(2));

    Contact contact = new Contact();
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    getAndValidatePublishedEvent(caseDetails, contact, "REPLACEMENT_UAC", "DUMMY_FULFILMENT");

    verifyRateLimiterCall(2, null, printRequest.getClientIP(), caseDetails);

    assertEquals(p1, productCaptor.getAllValues().get(0));
    assertEquals(p2, productCaptor.getAllValues().get(1));
  }

  // simulate RHUI continuation pages using same fulfilment code.
  @Test
  public void shouldFulfilRequestByPostForMultipleRepeatedFulfilmentCodes() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    caseSvc.fulfilmentRequestByPost(printFulfilmentRequestDTOS.get(3));

    Contact contact = new Contact();
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    getAndValidatePublishedEvent(
        caseDetails,
        contact,
        "REPLACEMENT_UAC",
        "DUMMY_FULFILMENT",
        "DUMMY_FULFILMENT",
        "DUMMY_FULFILMENT");

    verifyRateLimiterCall(4, null, printRequest.getClientIP(), caseDetails);

    assertEquals(p1, productCaptor.getAllValues().get(0));
    assertEquals(p2, productCaptor.getAllValues().get(1));
    assertEquals(p2, productCaptor.getAllValues().get(2));
    assertEquals(p2, productCaptor.getAllValues().get(3));
  }

  @Test
  public void shouldRejectPostalFulfilmentWhenRateLimiterRejects() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
        .when(rateLimiterClient)
        .checkFulfilmentRateLimit(any(), any(), any(), any(), any());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> caseSvc.fulfilmentRequestByPost(printRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any(EventPayload.class));
    verifyRateLimiterCall(1, null, printRequest.getClientIP(), caseDetails);
  }

  // multi sms fulfilment tests

  //  @Test
  //  public void shouldFulfilRequestBySmsForMultipleFulfilmentCodes() throws Exception {
  //    CaseUpdate caseDetails = selectCaseUpdateForTest(Product.CaseType.HH, false);
  //    String caseId = caseDetails.getCaseId();
  //    when(dataRepo.readCaseUpdate(eq(caseId))).thenReturn(Optional.of(caseDetails));
  //
  //    String phoneNo = "07714111222";
  //
  //    smsRequest.setTelNo(phoneNo);
  //    smsRequest.setCaseId(UUID.fromString(caseId));
  //    smsRequest.setFulfilmentCodes(Arrays.asList("F1", "F2", "F3"));
  //
  //    Product p1 = mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);
  //    Product p2 = mockProductSearch("F2", false, DeliveryChannel.SMS, Product.CaseType.HH);
  //    Product p3 = mockProductSearch("F3", false, DeliveryChannel.SMS, Product.CaseType.HH);
  //
  //    caseSvc.fulfilmentRequestBySMS(smsRequest);
  //
  //    Contact contact = new Contact();
  //    contact.setTelNo(phoneNo);
  //    getAndValidatePublishedEvent(caseDetails, contact, "F1", "F2", "F3");
  //
  //    verifyRateLimiterCall(3, phoneNo, smsRequest.getClientIP(), caseDetails);
  //
  //    assertEquals(p1, productCaptor.getAllValues().get(0));
  //    assertEquals(p2, productCaptor.getAllValues().get(1));
  //    assertEquals(p3, productCaptor.getAllValues().get(2));
  //  }
  //
  //  @Test
  //  public void shouldRejectSmsFulfilmentWhenRateLimiterRejects() throws Exception {
  //    CaseUpdate caseDetails = selectCaseUpdateForTest(Product.CaseType.HH, false);
  //    String caseId = caseDetails.getCaseId();
  //    when(dataRepo.readCaseUpdate(eq(caseId))).thenReturn(Optional.of(caseDetails));
  //
  //    doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
  //        .when(rateLimiterClient)
  //        .checkFulfilmentRateLimit(any(), any(), any(), any(), any());
  //
  //    String phoneNo = "07714111222";
  //    smsRequest.setCaseId(UUID.fromString(caseId));
  //    smsRequest.setTelNo(phoneNo);
  //    mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);
  //
  //    ResponseStatusException ex =
  //        assertThrows(
  //            ResponseStatusException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
  //
  //    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
  //    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any(EventPayload.class));
  //    verifyRateLimiterCall(1, phoneNo, smsRequest.getClientIP(), caseDetails);
  //  }

  // --- check when rate limiter turned off

  @Test
  public void shouldFulfilRequestByPostWhenRateLimiterNotEnabled() throws Exception {
    when(dataRepo.readCaseUpdate(any())).thenReturn(Optional.of(caseDetails));
    when(surveyRepository.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(false));

    caseSvc.fulfilmentRequestByPost(printRequest);
    verifyRateLimiterNotCalled();
  }

  //  @Test
  //  public void shouldFulfilRequestBySmsWhenRateLimiterNotEnabled() throws Exception {
  //    CaseUpdate caseDetails = selectCaseUpdateForTest(Product.CaseType.HH, false);
  //    String caseId = caseDetails.getCaseId();
  //    when(dataRepo.readCaseUpdate(eq(caseId))).thenReturn(Optional.of(caseDetails));
  //
  //    String phoneNo = "07714111222";
  //
  //    smsRequest.setTelNo(phoneNo);
  //    smsRequest.setCaseId(UUID.fromString(caseId));
  //    smsRequest.setFulfilmentCodes(Arrays.asList("F1"));
  //
  //    mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);
  //    when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(false));
  //
  //    caseSvc.fulfilmentRequestBySMS(smsRequest);
  //    verifyRateLimiterNotCalled();
  //  }

  // --- helpers

  private void verifyRateLimiterCall(
      int numTimes, String phoneNo, String clientIp, CaseUpdate caseDetails) throws Exception {
    UniquePropertyReferenceNumber uprn =
        UniquePropertyReferenceNumber.create(caseDetails.getSample().get("uprn"));
    verify(rateLimiterClient, times(numTimes))
        .checkFulfilmentRateLimit(
            eq(Domain.RH), productCaptor.capture(), eq(clientIp), eq(uprn), eq(phoneNo));
  }

  private void verifyRateLimiterNotCalled() throws Exception {
    verify(rateLimiterClient, never()).checkFulfilmentRateLimit(any(), any(), any(), any(), any());
  }

  private List<FulfilmentRequest> getAndValidatePublishedEvent(
      CaseUpdate caseDetails, Contact expectedContact, String... fulfilmentCodes) {
    ArgumentCaptor<TopicType> eventTypeCaptor = ArgumentCaptor.forClass(TopicType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<FulfilmentRequest> fulfilmentRequestCaptor =
        ArgumentCaptor.forClass(FulfilmentRequest.class);

    verify(eventPublisher, Mockito.times(fulfilmentCodes.length))
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            fulfilmentRequestCaptor.capture());

    List<FulfilmentRequest> events = new ArrayList<>();

    for (int i = 0; i < fulfilmentCodes.length; i++) {
      // Validate message routing
      assertEquals("FULFILMENT", eventTypeCaptor.getAllValues().get(i).toString());
      assertEquals("RESPONDENT_HOME", sourceCaptor.getAllValues().get(i).toString());
      assertEquals("RH", channelCaptor.getAllValues().get(i).toString());

      // Validate content of generated event
      FulfilmentRequest eventPayload = fulfilmentRequestCaptor.getAllValues().get(i);
      assertEquals(fulfilmentCodes[i], eventPayload.getPackCode());
      assertEquals(caseDetails.getCaseId(), eventPayload.getCaseId());
      assertEquals(
          expectedContact.getForename(),
          eventPayload.getPersonalisation().get("firstName").toString());
      assertEquals(
          expectedContact.getSurname(),
          eventPayload.getPersonalisation().get("lastName").toString());

      events.add(eventPayload);
    }
    return events;
  }
}
