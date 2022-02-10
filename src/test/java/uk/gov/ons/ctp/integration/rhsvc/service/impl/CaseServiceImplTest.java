package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.Sis;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;

@ExtendWith(MockitoExtension.class)
public class CaseServiceImplTest {

  private static final String UPRN = "123456";

  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private CaseRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  @Spy private AppConfig appConfig = new AppConfig();

  private List<CaseUpdate> caseUpdates;
  @Captor ArgumentCaptor<NewCasePayloadContent> sendEventCaptor;

  private List<NewCaseDTO> newCaseDTO;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.caseUpdates = FixtureHelper.loadClassFixtures(CaseUpdate[].class);
    this.newCaseDTO = FixtureHelper.loadClassFixtures(NewCaseDTO[].class);

    Sis sis = new Sis();
    sis.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    appConfig.setSis(sis);
    ReflectionTestUtils.setField(caseSvc, "appConfig", appConfig);
  }

  /** Test verifies searching which finds only a single results */
  @Test
  public void getCaseFoundWithSingleResult() throws Exception {

    when(dataRepo.findCaseUpdatesBySampleAttribute("uprn", UPRN, true))
        .thenReturn(caseUpdates.subList(0, 1));

    List<CaseDTO> rmCase = caseSvc.findCasesBySampleAttribute("uprn", UPRN);

    assertNotNull(rmCase);
    assertEquals(1, rmCase.size());
    verifyCase(caseUpdates.get(0), rmCase.get(0));
  }

  /** Test verifies searching which finds multiple results */
  @Test
  public void getCaseFoundWithMultipleResults() throws Exception {

    when(dataRepo.findCaseUpdatesBySampleAttribute("townName", "Upton", true))
        .thenReturn(caseUpdates);

    List<CaseDTO> rmCase = caseSvc.findCasesBySampleAttribute("townName", "Upton");

    assertNotNull(rmCase);
    assertEquals(2, rmCase.size());
    verifyCase(caseUpdates.get(0), rmCase.get(0));
    verifyCase(caseUpdates.get(1), rmCase.get(1));
  }

  private void verifyCase(CaseUpdate expectedCase, CaseDTO actualCase) {
    assertEquals(expectedCase.getCaseId(), actualCase.getCaseId().toString());
    assertEquals(expectedCase.getSurveyId(), actualCase.getSurveyId().toString());
    assertEquals(
        expectedCase.getCollectionExerciseId(), actualCase.getCollectionExerciseId().toString());
    assertEquals(expectedCase.isInvalid(), actualCase.isInvalid());
    assertEquals(expectedCase.getRefusalReceived(), actualCase.getRefusalReceived());
    assertEquals(expectedCase.getSample(), actualCase.getSample());
    assertEquals(expectedCase.getSampleSensitive(), actualCase.getSampleSensitive());
  }

  /** Test throws a CTPException where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {
    when(dataRepo.findCaseUpdatesBySampleAttribute("uprn", UPRN, true))
        .thenThrow(new CTPException(null));
    assertThrows(CTPException.class, () -> caseSvc.findCasesBySampleAttribute("uprn", UPRN));
  }

  /** Test for an empty result set when no cases returned from repository */
  @Test
  public void getCaseByUPRNNotFound() throws Exception {

    when(dataRepo.findCaseUpdatesBySampleAttribute("doorNumber", "898123", true))
        .thenReturn(new ArrayList<>());

    List<CaseDTO> foundCases = caseSvc.findCasesBySampleAttribute("doorNumber", "898123");
    assertTrue(foundCases.isEmpty());
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

    DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String expectedChildDOB = newCaseDTO.get(0).getChildDob().format(dobFormatter);

    UUID expectedCollectionExerciseId = UUID.fromString(COLLECTION_EXERCISE_ID);
    assertEquals(expectedCollectionExerciseId, eventPayload.getCollectionExerciseId());
    assertEquals(
        newCaseDTO.get(0).getSchoolId(),
        eventPayload.getSample().get(NewCasePayloadContent.ATTRIBUTE_SCHOOL_ID));
    assertEquals(
        newCaseDTO.get(0).getSchoolName(),
        eventPayload.getSample().get(NewCasePayloadContent.ATTRIBUTE_SCHOOL_NAME));
    assertEquals(
        newCaseDTO.get(0).getFirstName(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_FIRST_NAME));
    assertEquals(
        newCaseDTO.get(0).getLastName(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_LAST_NAME));
    assertEquals(
        newCaseDTO.get(0).getChildFirstName(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_CHILD_FIRST_NAME));
    assertEquals(
        newCaseDTO.get(0).getChildMiddleNames(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_CHILD_MIDDLE_NAMES));
    assertEquals(
        newCaseDTO.get(0).getChildLastName(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_CHILD_LAST_NAME));
    assertEquals(
        expectedChildDOB,
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_CHILD_DOB));
    assertEquals(
        newCaseDTO.get(0).getMobileNumber(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_MOBILE_NUMBER));
    assertEquals(
        newCaseDTO.get(0).getEmailAddress(),
        eventPayload.getSampleSensitive().get(NewCasePayloadContent.ATTRIBUTE_EMAIL_ADDRESS));
  }
}
