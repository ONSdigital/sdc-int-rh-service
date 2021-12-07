package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.ProductGroup;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;

@ExtendWith(MockitoExtension.class)
public class SurveyServiceImplTest {
  private static final List<String> SURVEY_IDS =
      Arrays.asList(
          "3883af91-0052-4497-9805-3238544fcf8a",
          "7645931e-542d-11ec-b825-4c3275913db5",
          "90c93916-542d-11ec-9c5e-4c3275913db5");

  @Mock private RespondentDataRepository dataRepo;
  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();
  @InjectMocks private SurveyServiceImpl service;

  @Test
  public void shouldRejectUnknownSurvey() throws Exception {
    when(dataRepo.readSurvey(any())).thenReturn(Optional.empty());
    CTPException e = assertThrows(CTPException.class, () -> service.survey(UUID.randomUUID()));
    assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
  }

  @Test
  public void shouldFindKnownSurveyWithNoFulfilments() throws Exception {
    SurveyUpdate surveyUpdate = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    surveyUpdate.setAllowedEmailFulfilments(null);
    surveyUpdate.setAllowedSmsFulfilments(null);
    surveyUpdate.setAllowedPrintFulfilments(new ArrayList<>());

    when(dataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    SurveyDTO dto = service.survey(UUID.randomUUID());
    assertNotNull(dto);
    assertEquals(SURVEY_IDS.get(0), dto.getSurveyId().toString());
    assertTrue(dto.getAllowedFulfilments().isEmpty());
  }

  @Test
  public void shouldFindKnownSurvey() throws Exception {
    SurveyUpdate surveyUpdate = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    when(dataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    SurveyDTO dto = service.survey(UUID.randomUUID());
    assertNotNull(dto);
    assertEquals(SURVEY_IDS.get(0), dto.getSurveyId().toString());
    assertEquals("LMS", dto.getName());
    assertEquals(SurveyType.SOCIAL, dto.getSurveyType());
    verifyProducts(dto);
  }

  private void verifyProducts(SurveyDTO dto) {
    var products = dto.getAllowedFulfilments();
    assertNotNull(products);
    assertEquals(5, products.size());
    assertEquals(0, countInChannel(DeliveryChannel.EMAIL, products));
    assertEquals(2, countInChannel(DeliveryChannel.SMS, products));
    assertEquals(3, countInChannel(DeliveryChannel.POST, products));

    // check one of the products ...
    var prod =
        products.stream()
            .filter(
                p ->
                    p.getDeliveryChannel() == DeliveryChannel.SMS
                        && "replace-uac-en".equals(p.getPackCode()))
            .findFirst()
            .orElse(null);
    assertNotNull(prod);
    assertEquals(ProductGroup.UAC, prod.getProductGroup());
    var meta = prod.getMetadata();
    var regions = meta.get("suitableRegions");
    assertNotNull(regions);
    assertEquals(regions, Arrays.asList("E", "N"));
  }

  private long countInChannel(DeliveryChannel channel, List<ProductDTO> products) {
    return products.stream().filter(p -> p.getDeliveryChannel() == channel).count();
  }

  @Test
  public void shouldFindNoSurveys() throws Exception {
    when(dataRepo.listSurveys()).thenReturn(Collections.emptyList());
    List<SurveyDTO> surveys = service.listSurveys();
    assertTrue(surveys.isEmpty());
  }

  @Test
  public void shouldFindSurveys() throws Exception {
    var surveyUpdates = FixtureHelper.loadClassFixtures(SurveyUpdate[].class);
    when(dataRepo.listSurveys()).thenReturn(surveyUpdates);
    List<SurveyDTO> surveys = service.listSurveys();
    assertEquals(3, surveys.size());

    for (int i = 0; i < 3; i++) {
      assertEquals(SURVEY_IDS.get(i), surveys.get(i).getSurveyId().toString());
    }
  }
}
