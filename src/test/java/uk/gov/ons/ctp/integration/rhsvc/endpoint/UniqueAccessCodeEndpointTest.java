package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACContextDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.UniqueAccessCodeServiceImpl;

/** Unit Tests on endpoint for UAC resources */
@ExtendWith(MockitoExtension.class)
public class UniqueAccessCodeEndpointTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";
  private static final String QID = "1110000009";
  private static final String POSTCODE = "UP103UP";
  private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
  private static final String ERROR_MESSAGE = "Failed to retrieve UAC";

  @InjectMocks private UniqueAccessCodeEndpoint uacEndpoint;

  @Mock UniqueAccessCodeServiceImpl uacService;

  private MockMvc mockMvc;

  private List<UACContextDTO> uacDTO;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(uacEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
    this.uacDTO = FixtureHelper.loadClassFixtures(UACContextDTO[].class);
  }

  /** Test returns valid JSON for valid UAC */
  @Test
  public void getUACClaimContext_UACFound() throws Exception {
    when(uacService.getUACClaimContext(UAC_HASH)).thenReturn(uacDTO.get(0));

    String COLLECTION_EXERCISE_ID = "4883af91-0052-4497-9805-3238544fcf8a";
    String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";
    mockMvc
        .perform(get(String.format("/uacs/%s", UAC_HASH)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.uacHash", is(UAC_HASH)))
        .andExpect(jsonPath("$.collectionCase.caseId", is(CASE_ID)))
        .andExpect(jsonPath("$.survey.surveyId", is(SURVEY_ID)))
        .andExpect(
            jsonPath("$.collectionExercise.collectionExerciseId", is(COLLECTION_EXERCISE_ID)))
        .andExpect(jsonPath("$.qid", is(QID)))
        .andExpect(jsonPath("$.collectionCase.sample.postcode", is(POSTCODE)))
        .andExpect(jsonPath("$.receiptReceived", is(Boolean.TRUE)))
        .andExpect(jsonPath("$.eqLaunched", is(Boolean.TRUE)))
        .andExpect(jsonPath("$.wave", is(82)));
  }

  /** Test returns resource not found for invalid UAC */
  @Test
  public void getUACClaimContext_UACNotFound() throws Exception {
    when(uacService.getUACClaimContext(UAC_HASH))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(get("/uacs/{uac}", UAC_HASH))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(ERROR_CODE)))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }

  @Test
  public void generateEqLaunchToken_happyPath() throws Exception {
    when(uacService.generateEqLaunchToken(eq(UAC_HASH), any())).thenReturn("an-eq-launch-url");

    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?languageCode=en&accountServiceUrl=/service_url&accountServiceLogoutUrl=/logout_url&clientIP=1.2.3.4",
                UAC_HASH))
        .andExpect(status().isOk())
        .andExpect(content().string("an-eq-launch-url"));
  }

  @Test
  public void generateEqLaunchToken_invalidLanguage() throws Exception {
    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?"
                    + "languageCode=french&"
                    + "accountServiceUrl=/service_url&"
                    + "accountServiceLogoutUrl=/logout_url&"
                    + "clientIP=1.2.3.4",
                UAC_HASH))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void generateEqLaunchToken_noLanguage() throws Exception {
    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?"
                    + "accountServiceUrl=service_url&"
                    + "accountServiceLogoutUrl=logout_url&"
                    + "clientIP=1.2.3.4",
                UAC_HASH))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void generateEqLaunchToken_noAccountServiceUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?"
                    + "languageCode=french&"
                    + "accountServiceLogoutUrl=/logout_url&"
                    + "clientIP=1.2.3.4",
                UAC_HASH))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void generateEqLaunchToken_noAccountServiceLogoutUrl() throws Exception {
    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?"
                    + "languageCode=french&"
                    + "accountServiceUrl=/service_url&"
                    + "clientIP=1.2.3.4",
                UAC_HASH))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void generateEqLaunchToken_noClientIP() throws Exception {
    mockMvc
        .perform(
            get(
                "/uacs/{uac}/launch?"
                    + "languageCode=french&"
                    + "accountServiceUrl=/service_url&"
                    + "accountServiceLogoutUrl=/logout_url&",
                UAC_HASH))
        .andExpect(status().isBadRequest());
  }
}
