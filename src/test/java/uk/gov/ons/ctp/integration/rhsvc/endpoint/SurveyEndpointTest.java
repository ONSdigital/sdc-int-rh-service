package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyService;

@ExtendWith(MockitoExtension.class)
public class SurveyEndpointTest {
  private static final String UNKNOWN_SURVEY_ID = "423a89ce-537b-11ec-b656-4c3275913db5";
  private static final String SURVEY_ID_1 = "91b32904-5360-11ec-b6f8-4c3275913db5";
  private static final String SURVEY_ID_2 = "c552e4b6-5360-11ec-a2e7-4c3275913db5";

  @Mock SurveyService service;
  @InjectMocks private SurveyEndpoint endpoint;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(endpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
  }

  @Test
  public void shouldReturnEmptyListWhenNoSurveys() throws Exception {
    List<SurveyDTO> surveys = Collections.emptyList();
    when(service.listSurveys()).thenReturn(surveys);
    mockMvc
        .perform(get("/surveys"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(content().json("[]"));
  }

  @Test
  public void shouldReturnListOfSurveys() throws Exception {
    List<SurveyDTO> surveys = FixtureHelper.loadClassFixtures(SurveyDTO[].class);
    when(service.listSurveys()).thenReturn(surveys);
    mockMvc
        .perform(get("/surveys"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.length()", is(2)));
  }

  @Test
  public void shouldFailToFindSurvey() throws Exception {
    when(service.survey(any())).thenThrow(new CTPException(Fault.RESOURCE_NOT_FOUND));
    mockMvc.perform(get("/surveys/" + UNKNOWN_SURVEY_ID)).andExpect(status().isNotFound());
  }

  @Test
  public void shouldFindKnownSurvey() throws Exception {
    SurveyDTO survey = FixtureHelper.loadClassFixtures(SurveyDTO[].class).get(0);
    when(service.survey(ArgumentMatchers.eq(UUID.fromString(SURVEY_ID_1)))).thenReturn(survey);

    mockMvc
        .perform(get("/surveys/" + SURVEY_ID_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.surveyId", is(SURVEY_ID_1)))
        .andExpect(jsonPath("$.name", is("how-rich-are-you")))
        .andExpect(jsonPath("$.surveyType", is(SurveyType.SOCIAL.name())))
        .andExpect(jsonPath("$.allowedFulfilments.length()", is(1)))
        .andExpect(jsonPath("$.allowedFulfilments[0].packCode", is("replace-uac-en")))
        .andExpect(jsonPath("$.allowedFulfilments[0].description", is("English UAC thing")))
        .andExpect(jsonPath("$.allowedFulfilments[0].productGroup", is("UAC")))
        .andExpect(jsonPath("$.allowedFulfilments[0].deliveryChannel", is("SMS")))
        .andExpect(jsonPath("$.allowedFulfilments[0].metadata.abc", is("def")))
        .andExpect(jsonPath("$.allowedFulfilments[0].metadata.valid", is(true)));
  }

  @Test
  public void shouldFindKnownSurveyWithMultipleProducts() throws Exception {
    SurveyDTO survey = FixtureHelper.loadClassFixtures(SurveyDTO[].class).get(1);
    when(service.survey(ArgumentMatchers.eq(UUID.fromString(SURVEY_ID_2)))).thenReturn(survey);

    mockMvc
        .perform(get("/surveys/" + SURVEY_ID_2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.surveyId", is(SURVEY_ID_2)))
        .andExpect(jsonPath("$.name", is("handbag-preferences")))
        .andExpect(jsonPath("$.surveyType", is(SurveyType.SIS.name())))
        .andExpect(jsonPath("$.allowedFulfilments.length()", is(2)))
        .andExpect(jsonPath("$.allowedFulfilments[0].packCode", is("pc1")))
        .andExpect(jsonPath("$.allowedFulfilments[1].packCode", is("pc2")));
  }
}
