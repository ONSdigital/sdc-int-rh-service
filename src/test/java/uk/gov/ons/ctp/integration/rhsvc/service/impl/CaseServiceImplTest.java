package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;

@ExtendWith(MockitoExtension.class)
public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  @Spy private AppConfig appConfig = new AppConfig();

  @Captor ArgumentCaptor<NewCasePayloadContent> sendEventCaptor;

  private List<CollectionCase> collectionCase;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);

    appConfig.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    ReflectionTestUtils.setField(caseSvc, "appConfig", appConfig);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {

    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.of(collectionCase.get(0)));

    CollectionCase nonHICase = this.collectionCase.get(0);

    CaseDTO rmCase = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertNotNull(rmCase);
    assertEquals(nonHICase.getId(), rmCase.getCaseId().toString());
    assertEquals(nonHICase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(nonHICase.getCaseType(), rmCase.getCaseType());
    assertEquals(nonHICase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(nonHICase.getAddress().getAddressLine1(), rmCase.getAddress().getAddressLine1());
    assertEquals(nonHICase.getAddress().getAddressLine2(), rmCase.getAddress().getAddressLine2());
    assertEquals(nonHICase.getAddress().getAddressLine3(), rmCase.getAddress().getAddressLine3());
    assertEquals(nonHICase.getAddress().getTownName(), rmCase.getAddress().getTownName());
    assertEquals(nonHICase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(nonHICase.getAddress().getPostcode(), rmCase.getAddress().getPostcode());
    assertEquals(
        nonHICase.getAddress().getUprn(), Long.toString(rmCase.getAddress().getUprn().getValue()));
  }

  /** Test throws a CTPException where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {
    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenThrow(new CTPException(null));
    assertThrows(CTPException.class, () -> caseSvc.getLatestValidNonHICaseByUPRN(UPRN));
  }

  /** Test retrieves latest case when all valid HH */
  @Test
  public void getLatestCaseByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);

    collectionCase.forEach(cc -> cc.setCaseType("HH"));

    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(1).setCreatedDateTime(latest); // EXPECTED
    collectionCase.get(2).setCreatedDateTime(earliest);
    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.of(collectionCase.get(1)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        UUID.fromString(collectionCase.get(1).getId()),
        result.getCaseId(),
        "Resultant Case created date should match expected case with latest date");
  }

  /** Test retrieves latest valid case when actual latest date is an HI case */
  @Test
  public void getLatestCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH"); // EXPECTED
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID
    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.of(collectionCase.get(0)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        UUID.fromString(collectionCase.get(0).getId()),
        result.getCaseId(),
        "Resultant Case created date should match expected case with latest date");
  }

  /** Test retrieves latest Address valid case when actual latest date is an HI case */
  @Test
  public void getLatestAddressValidCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH");
    collectionCase.get(0).setAddressInvalid(Boolean.TRUE); // INVALID
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID / EXPECTED
    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.of(collectionCase.get(2)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        UUID.fromString(collectionCase.get(2).getId()),
        result.getCaseId(),
        "Resultant Case created date should match expected case with latest date and Valid Address");
  }

  /** Test Test throws a CTPException where no cases returned from repository */
  @Test
  public void getCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readNonHILatestCollectionCaseByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.empty());

    assertThrows(CTPException.class, () -> caseSvc.getLatestValidNonHICaseByUPRN(UPRN));
  }

  @Test
  public void shouldCreateNewCaseRequestPayload() throws Exception {
    callAndVerifyNewCaseCreated();
  }

  private void callAndVerifyNewCaseCreated() throws CTPException {

    NewCaseDTO newCaseDTO = getNewCaseDto();

    caseSvc.sendNewCaseEvent(newCaseDTO);

    verify(eventPublisher)
        .sendEvent(
            eq(EventType.NEW_CASE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            sendEventCaptor.capture());
    NewCasePayloadContent eventPayload = sendEventCaptor.getValue();

    assertEquals(newCaseDTO.getCollectionExerciseId(), eventPayload.getCollectionExerciseId());
    assertEquals(newCaseDTO.getSchoolId(), eventPayload.getSample().getSchoolId());
    assertEquals(newCaseDTO.getSchoolName(), eventPayload.getSample().getSchoolName());
    assertEquals(newCaseDTO.getFirstName(), eventPayload.getSampleSensitive().getFirstName());
    assertEquals(newCaseDTO.getLastName(), eventPayload.getSampleSensitive().getLastName());
    assertEquals(
        newCaseDTO.getChildFirstName(), eventPayload.getSampleSensitive().getChildFirstName());
    assertEquals(
        newCaseDTO.getChildMiddleName(), eventPayload.getSampleSensitive().getChildMiddleNames());
    assertEquals(
        newCaseDTO.getChildLastName(), eventPayload.getSampleSensitive().getChildLastName());
    assertEquals(newCaseDTO.getChildDob(), eventPayload.getSampleSensitive().getChildDob());
    assertEquals(
        newCaseDTO.getParentMobileNumber(),
        eventPayload.getSampleSensitive().getParentMobileNumber());
    assertEquals(
        newCaseDTO.getParentEmailAddress(),
        eventPayload.getSampleSensitive().getParentEmailAddress());
  }

  public NewCaseDTO getNewCaseDto() {
    NewCaseDTO newCaseDTO = new NewCaseDTO();

    newCaseDTO.setCollectionExerciseId(UUID.fromString("22684ede-7d5f-4f53-9069-2398055c61b2"));
    newCaseDTO.setSchoolId("abc1234");
    newCaseDTO.setSchoolName("Chesterthorps High School");
    newCaseDTO.setFirstName("Fred");
    newCaseDTO.setLastName("Bloggs");
    newCaseDTO.setChildFirstName("Jo");
    newCaseDTO.setChildMiddleName("Ross");
    newCaseDTO.setChildLastName("Bloggs");
    newCaseDTO.setChildDob(LocalDate.parse("2001-12-31"));
    newCaseDTO.setParentMobileNumber("447123456999");
    newCaseDTO.setParentEmailAddress("fred.bloggs@domain.com");

    return newCaseDTO;
  }
}
