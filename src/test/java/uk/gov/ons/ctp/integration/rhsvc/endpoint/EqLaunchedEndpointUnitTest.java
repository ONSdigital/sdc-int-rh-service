package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.EqLaunchedService;

/** Respondent Home Endpoint Unit tests */
@ExtendWith(MockitoExtension.class)
public final class EqLaunchedEndpointUnitTest {
  @InjectMocks private EqLaunchedEndpoint eqLaunchedEndpoint;

  @Mock EqLaunchedService eqLaunchedService;

  private ObjectMapper mapper = new ObjectMapper();

  private MockMvc mockMvc;
  private EqLaunchDTO dto;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @BeforeEach
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(eqLaunchedEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
    dto = FixtureHelper.loadClassFixtures(EqLaunchDTO[].class).get(0);
  }

  @Test
  public void eqLaunchedSuccessCaseEmptyString() throws Exception {
    Mockito.doNothing().when(eqLaunchedService).eqLaunched(any());
    callEndpointExpectingSuccess();
  }

  @Test
  public void eqLaunchedSuccessCaseAssistedDigitalLocation() throws Exception {
    Mockito.doNothing().when(eqLaunchedService).eqLaunched(any());
    callEndpointExpectingSuccess();
  }

  @Test
  public void shouldRejectMissingQuestionnaireId() throws Exception {
    dto.setQuestionnaireId(null);
    callEndpointExpectingBadRequest();
  }

  @Test
  public void shouldLaunchWithMissingClientIP() throws Exception {
    dto.setClientIP(null);
    callEndpointExpectingSuccess();
  }

  @Test
  public void shouldRejectInvalidCaseId() throws Exception {
    String eqLaunchedRequestBody =
        "{ \"questionnaireId\": \"23434234234\",   \"caseId\": \"euieuieu@#$@#$\" }";
    callEndpointExpectingBadRequest(eqLaunchedRequestBody);
  }

  @Test
  public void shouldRejectInvalidRequest() throws Exception {
    String eqLaunchedRequestBody = "uoeuoeu 45345345 euieuiaooo";
    callEndpointExpectingBadRequest(eqLaunchedRequestBody);
  }

  private void callEndpointExpectingSuccess() throws Exception {
    mockMvc
        .perform(postJson("/surveyLaunched", mapper.writeValueAsString(dto)))
        .andExpect(status().isOk());
  }

  private void callEndpointExpectingBadRequest() throws Exception {
    callEndpointExpectingBadRequest(mapper.writeValueAsString(dto));
  }

  private void callEndpointExpectingBadRequest(String body) throws Exception {
    mockMvc.perform(postJson("/surveyLaunched", body)).andExpect(status().isBadRequest());
  }
}
