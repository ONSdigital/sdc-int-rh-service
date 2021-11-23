package uk.gov.ons.ctp.integration.rhsvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

@DisplayName("RHSvc Bean Mapper Test")
public class RHSvcBeanMapperTest {

  private MapperFacade mapper = new RHSvcBeanMapper();

  private CaseUpdate caseUpdate;
  private UacUpdate uacUpdate;
  private SurveyUpdate surveyUpdate;
  private CollectionExercise collectionExercise;

  @BeforeEach
  public void setup() {
    caseUpdate = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    uacUpdate = FixtureHelper.loadClassFixtures(UacUpdate[].class).get(0);
    surveyUpdate = FixtureHelper.loadClassFixtures(SurveyUpdate[].class).get(0);
    collectionExercise = FixtureHelper.loadClassFixtures(CollectionExercise[].class).get(0);
  }

  private AddressDTO expectedAddress() {
    AddressDTO addr = new AddressDTO();
    addr.setAddressLine1("1 main street");
    addr.setAddressLine2("upper upperingham");
    addr.setAddressLine3("");
    addr.setTownName("upton");
    addr.setPostcode("UP103UP");
    addr.setUprn(UniquePropertyReferenceNumber.create("123456"));
    addr.setRegion(Region.E);
    return addr;
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
    assertEquals(expectedAddress(), dto.getAddress());
    assertEquals(caseUpdate.getSample().getRegion(), dto.getAddress().getRegion().toString());
  }

  @Test
  @DisplayName("UacUpdate -> UniqueAccessCodeDTO mapping")
  public void shouldMapUacUpdateToUniqueAccessCodeDTO() {
    UniqueAccessCodeDTO dto = mapper.map(uacUpdate, UniqueAccessCodeDTO.class);
    assertTrue(dto.isActive());
    assertEquals(
        "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4", dto.getUacHash());
    assertEquals("1110000009", dto.getQid());
    assertTrue(dto.isReceiptReceived());
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
  @DisplayName("SurveyUpdate -> urveyLiteDTO mapping")
  public void shouldMapSurveyUpdateToSurveyLiteDTO() {
    SurveyLiteDTO dto = mapper.map(surveyUpdate, SurveyLiteDTO.class);
    assertEquals(UUID.fromString("3883af91-0052-4497-9805-3238544fcf8a"), dto.getSurveyId());
    assertEquals("LMS", dto.getName());
  }
}
