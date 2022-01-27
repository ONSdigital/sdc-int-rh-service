package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.LaunchDataDTO;

@ExtendWith(MockitoExtension.class)
public class EqLaunchedServiceImplTest {

  private AppConfig appConfig = mock(AppConfig.class, Mockito.RETURNS_DEEP_STUBS);

  @Mock private UacUpdate uacUpdate;
  @Mock private CaseUpdate caseUpdate;
  @Mock private CollectionExerciseUpdate collectionExerciseUpdate;
  @Mock private SurveyUpdate surveyUpdate;

  @Mock private EqLaunchService eqLaunchService;

  @InjectMocks EqLaunchServiceImpl eqLaunchedService;

  @Captor ArgumentCaptor<EqLaunchData> eqLaunchDataCaptor;

  @BeforeEach
  public void setup() {
    when(appConfig.getEq().getProtocol()).thenReturn("https");
    when(appConfig.getEq().getHost()).thenReturn("www.google.com");
    when(appConfig.getEq().getPath()).thenReturn("/en/start/launch-eq/?token=");

    when(appConfig.getEq().getResponseIdSalt()).thenReturn("123");

    when(surveyUpdate.getSampleDefinitionUrl()).thenReturn("social.json");
  }

  @Test
  public void testEqLaunchedAddressAgentIdValue() throws Exception {
    LaunchDataDTO launchData =
        LaunchDataDTO.builder()
            .uacUpdate(uacUpdate)
            .caseUpdate(caseUpdate)
            .collectionExerciseUpdate(collectionExerciseUpdate)
            .surveyUpdate(surveyUpdate)
            .build();

    EqLaunchRequestDTO eqLaunchDTO =
        EqLaunchRequestDTO.builder()
            .languageCode(Language.WELSH)
            .accountServiceUrl("/accountServiceUrl")
            .accountServiceLogoutUrl("/accountServiceLogoutUrl")
            .clientIP("11.22.33.44")
            .build();

    when(eqLaunchService.getEqLaunchJwe(any())).thenReturn("eyJraWQiOiIx...");

    // Invoke code under test
    String eqLaunchURL = eqLaunchedService.createLaunchUrl(launchData, eqLaunchDTO);

    assertEquals("https://www.google.com/en/start/launch-eq/?token=eyJraWQiOiIx...", eqLaunchURL);

    // Verify claims information passed to the eq-launcher
    verify(eqLaunchService).getEqLaunchJwe(eqLaunchDataCaptor.capture());
    EqLaunchData eqLaunchData = eqLaunchDataCaptor.getValue();
    assertEquals(Channel.RH, eqLaunchData.getChannel());
    // TODO: Consider testing all fields of eqLaunchData once we have confirmed that the url is
    // correctly constructed
  }
}
