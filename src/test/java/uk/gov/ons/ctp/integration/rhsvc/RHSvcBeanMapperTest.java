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
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

@DisplayName("RHSvc Bean Mapper Test")
public class RHSvcBeanMapperTest {

  private MapperFacade mapper = new RHSvcBeanMapper();

  private CaseUpdate caseUpdate;
  private UacUpdate uacUpdate;

  @BeforeEach
  public void setup() {
    caseUpdate = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    uacUpdate = FixtureHelper.loadClassFixtures(UacUpdate[].class).get(0);
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

    assertEquals(UUID.fromString("bfb5cdca-3119-4d2c-a807-51ae55443b33"), dto.getCaseId());
    assertTrue(dto.isActive());
    assertEquals(
        "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4", dto.getUacHash());
    assertEquals("1110000009", dto.getQid());
    assertTrue(dto.isReceiptReceived());
    assertEquals(94, dto.getWave());
    assertTrue(dto.isEqLaunched());
  }
}
