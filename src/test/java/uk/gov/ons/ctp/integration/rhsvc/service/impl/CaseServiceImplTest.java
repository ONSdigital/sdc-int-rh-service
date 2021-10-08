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
import uk.gov.ons.ctp.common.event.TopicType;
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

  private List<NewCaseDTO> newCaseDTO;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.newCaseDTO = FixtureHelper.loadClassFixtures(NewCaseDTO[].class);

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

    returnNewCaseDTO();

    caseSvc.sendNewCaseEvent(newCaseDTO.get(0));

    verify(eventPublisher)
        .sendEvent(
            eq(TopicType.NEW_CASE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            sendEventCaptor.capture());
    NewCasePayloadContent eventPayload = sendEventCaptor.getValue();

    assertEquals(
        newCaseDTO.get(0).getCollectionExerciseId(), eventPayload.getCollectionExerciseId());
    assertEquals(newCaseDTO.get(0).getSchoolId(), eventPayload.getSample().getSchoolId());
    assertEquals(newCaseDTO.get(0).getSchoolName(), eventPayload.getSample().getSchoolName());
    assertEquals(
        newCaseDTO.get(0).getFirstName(), eventPayload.getSampleSensitive().getFirstName());
    assertEquals(newCaseDTO.get(0).getLastName(), eventPayload.getSampleSensitive().getLastName());
    assertEquals(
        newCaseDTO.get(0).getChildFirstName(),
        eventPayload.getSampleSensitive().getChildFirstName());
    assertEquals(
        newCaseDTO.get(0).getChildMiddleName(),
        eventPayload.getSampleSensitive().getChildMiddleNames());
    assertEquals(
        newCaseDTO.get(0).getChildLastName(), eventPayload.getSampleSensitive().getChildLastName());
    assertEquals(newCaseDTO.get(0).getChildDob(), eventPayload.getSampleSensitive().getChildDob());
    assertEquals(
        newCaseDTO.get(0).getParentMobileNumber(),
        eventPayload.getSampleSensitive().getParentMobileNumber());
    assertEquals(
        newCaseDTO.get(0).getParentEmailAddress(),
        eventPayload.getSampleSensitive().getParentEmailAddress());
  }

  private void returnNewCaseDTO() {
    newCaseDTO
        .get(0)
        .setCollectionExerciseId(UUID.fromString("22684ede-7d5f-4f53-9069-2398055c61b2"));
    newCaseDTO.get(0).setSchoolId("abc1234");
    newCaseDTO.get(0).setSchoolName("Chesterthorps High School");
    newCaseDTO.get(0).setFirstName("Fred");
    newCaseDTO.get(0).setLastName("Bloggs");
    newCaseDTO.get(0).setChildFirstName("Jo");
    newCaseDTO.get(0).setChildMiddleName("Ross");
    newCaseDTO.get(0).setChildLastName("Bloggs");
    newCaseDTO.get(0).setChildDob(LocalDate.parse("2001-12-31"));
    newCaseDTO.get(0).setParentMobileNumber("447123456999");
    newCaseDTO.get(0).setParentEmailAddress("fred.bloggs@domain.com");
  }
}
