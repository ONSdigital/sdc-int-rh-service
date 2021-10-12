package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import ma.glasnost.orika.MapperFacade;
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
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
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

  private List<CaseUpdate> caseUpdate;
  @Captor ArgumentCaptor<NewCasePayloadContent> sendEventCaptor;

  private List<NewCaseDTO> newCaseDTO;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.caseUpdate = FixtureHelper.loadClassFixtures(CaseUpdate[].class);
    this.newCaseDTO = FixtureHelper.loadClassFixtures(NewCaseDTO[].class);

    appConfig.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    ReflectionTestUtils.setField(caseSvc, "appConfig", appConfig);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {

    when(dataRepo.readCaseUpdateByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.of(caseUpdate.get(0)));

    CaseUpdate caseUpdate = this.caseUpdate.get(0);

    CaseDTO rmCase = caseSvc.getLatestValidCaseByUPRN(UPRN);

    assertNotNull(rmCase);
    assertEquals(caseUpdate.getCaseId(), rmCase.getCaseId().toString());
    assertEquals(caseUpdate.getSurveyId(), rmCase.getSurveyId().toString());
    assertEquals(caseUpdate.getCollectionExerciseId(), rmCase.getCollectionExerciseId().toString());
    assertEquals(caseUpdate.isInvalid(), rmCase.isInvalid());
    assertEquals(caseUpdate.getRefusalReceived(), rmCase.getRefusalReceived());
    assertEquals(caseUpdate.getSample().getAddressLine1(), rmCase.getAddress().getAddressLine1());
    assertEquals(caseUpdate.getSample().getAddressLine2(), rmCase.getAddress().getAddressLine2());
    assertEquals(caseUpdate.getSample().getAddressLine3(), rmCase.getAddress().getAddressLine3());
    assertEquals(caseUpdate.getSample().getTownName(), rmCase.getAddress().getTownName());
    assertEquals(caseUpdate.getSample().getRegion(), rmCase.getAddress().getRegion().name());
    assertEquals(caseUpdate.getSample().getPostcode(), rmCase.getAddress().getPostcode());
    assertEquals(
        caseUpdate.getSample().getUprn(), Long.toString(rmCase.getAddress().getUprn().getValue()));
  }

  /** Test throws a CTPException where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {
    when(dataRepo.readCaseUpdateByUprn(Long.toString(UPRN.getValue()), true))
        .thenThrow(new CTPException(null));
    assertThrows(CTPException.class, () -> caseSvc.getLatestValidCaseByUPRN(UPRN));
  }

  /** Test Test throws a CTPException where no cases returned from repository */
  @Test
  public void getCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readCaseUpdateByUprn(Long.toString(UPRN.getValue()), true))
        .thenReturn(Optional.empty());

    assertThrows(CTPException.class, () -> caseSvc.getLatestValidCaseByUPRN(UPRN));
  }

  @Test
  public void shouldCreateNewCaseRequestPayload() throws Exception {
    callAndVerifyNewCaseCreated();
  }

  private void callAndVerifyNewCaseCreated() throws CTPException {

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
        newCaseDTO.get(0).getChildMiddleNames(),
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
}
