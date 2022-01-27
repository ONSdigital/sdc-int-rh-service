package uk.gov.ons.ctp.integration.rhsvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACContextDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;

@DisplayName("RHSvc Bean Mapper Test")
public class RHSvcBeanMapperTest {

  private MapperFacade mapper = new RHSvcBeanMapper();

  private CaseUpdate caseUpdate;
  private UacUpdate uacUpdate;
  private SurveyUpdate surveyUpdate;
  private CollectionExerciseUpdate collectionExercise;

  @BeforeEach
  public void setup() {
    caseUpdate = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    uacUpdate = FixtureHelper.loadClassFixtures(UacUpdate[].class).get(0);
    surveyUpdate = FixtureHelper.loadClassFixtures(SurveyUpdate[].class).get(0);
    collectionExercise = FixtureHelper.loadClassFixtures(CollectionExerciseUpdate[].class).get(0);
  }

  @Test
  @DisplayName("CaseUpdate -> CaseDTO mapping")
  public void shouldMapCaseUpdateToCaseDTO() {
    CaseDTO dto = mapper.map(caseUpdate, CaseDTO.class);
    assertEquals(UUID.fromString("aa4477d1-dd3f-4c69-b181-7ff725dc9fa4"), dto.getCaseId());
    assertEquals(UUID.fromString("a66de4dc-3c3b-11e9-b210-d663bd873d93"), dto.getSurveyId());
    assertEquals(
        UUID.fromString("a66de4dc-3c3b-11e9-b210-d663bd873d93"), dto.getCollectionExerciseId());
    assertEquals("HARD_REFUSAL", dto.getRefusalReceived());
    assertEquals("UP103UP", dto.getSample().get("postcode"));
    assertEquals("REDACTED", dto.getSampleSensitive().get("phoneNumber"));
  }

  @Test
  @DisplayName("UacUpdate -> UniqueAccessCodeDTO mapping")
  public void shouldMapUacUpdateToUniqueAccessCodeDTO() {
    UACContextDTO dto = mapper.map(uacUpdate, UACContextDTO.class);
    assertTrue(dto.isActive());
    assertEquals(
        "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4", dto.getUacHash());
    assertEquals("1110000009", dto.getQid());
    assertTrue(dto.isReceiptReceived());
    assertEquals(uacUpdate.getCollectionInstrumentUrl(), dto.getCollectionInstrumentUrl());
    assertEquals(94, dto.getWave());
    assertTrue(dto.isEqLaunched());
  }

  @Test
  @DisplayName("CollectionExercise -> CollectionExerciseDTO mapping")
  public void shouldMapCollectionExerciseToCollectionExerciseDTO() {
    CollectionExerciseDTO dto = mapper.map(collectionExercise, CollectionExerciseDTO.class);
    assertEquals(
        UUID.fromString("44d7f3bb-91c9-45d0-bb2d-90afce4fc790"), dto.getCollectionExerciseId());
    assertEquals(UUID.fromString("3883af91-0052-4497-9805-3238544fcf8a"), dto.getSurveyId());
    assertEquals("velit", dto.getName());
    assertEquals("MVP012021", dto.getReference());
    assertEquals("2021-09-17T23:59:59.999", dto.getStartDate().toString());
    assertEquals("2021-09-27T23:59:59.999", dto.getEndDate().toString());
    assertEquals(3, dto.getNumberOfWaves());
    assertEquals(2, dto.getWaveLength());
    assertEquals(3, dto.getCohorts());
    assertEquals(7, dto.getCohortSchedule());
  }

  @Test
  @DisplayName("SurveyUpdate -> SurveyLiteDTO mapping")
  public void shouldMapSurveyUpdateToSurveyLiteDTO() {
    SurveyLiteDTO dto = mapper.map(surveyUpdate, SurveyLiteDTO.class);
    assertEquals(UUID.fromString("3883af91-0052-4497-9805-3238544fcf8a"), dto.getSurveyId());
    assertEquals("LMS", dto.getName());
  }

  @Test
  @DisplayName("SurveyFulfilment -> ProductDTO mapping")
  public void shouldMapSurveyFulfilmentToProductDTO() {
    SurveyFulfilment fulfilment = FixtureHelper.loadClassFixtures(SurveyFulfilment[].class).get(0);
    ProductDTO dto = mapper.map(fulfilment, ProductDTO.class);
    var metadata = dto.getMetadata();
    var expectedRegions = Arrays.asList("E", "N");
    assertAll(
        () -> assertEquals("replace-uac-en", dto.getPackCode()),
        () -> assertEquals("Replacement UAC - English", dto.getDescription()),
        () -> assertEquals(metadata.get("suitableRegions"), expectedRegions));
  }

  @Test
  @DisplayName("LocalDateTime -> Date")
  public void convertLocalDateTimeToDatetest() {
    Date date = mapper.map(LocalDateTime.parse("2021-09-17T23:59:59.999"), Date.class);
    assertEquals(Date.from(Instant.parse("2021-09-17T23:59:59.999Z")), date);
  }
}
