package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
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
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PrintFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.CaseServiceImpl;

// ** Unit Tests on endpoint for Case resources */
@ExtendWith(MockitoExtension.class)
public class CaseEndpointUnitTest {

  private static final String UPRN = "123456";
  private static final String INCONSISTENT_CASEID = "ff9999f9-ff9f-9f99-f999-9ff999ff9ff9";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseServiceImpl caseService;

  private MockMvc mockMvc;

  /** Setup tests */
  @BeforeEach
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
  }

  /** Test returns valid JSON for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {
    List<CaseDTO> caseDTO = FixtureHelper.loadClassFixtures(CaseDTO[].class);
    CaseDTO rmCase0 = caseDTO.get(0);
    CaseDTO rmCase1 = caseDTO.get(1);

    when(caseService.findCasesBySampleAttribute("uprn", UPRN)).thenReturn(caseDTO);

    mockMvc
        .perform(get("/cases/attribute/uprn/{uprn}", UPRN))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].caseId", is(rmCase0.getCaseId().toString())))
        .andExpect(
            jsonPath("$[0].sample.addressLine1", is(rmCase0.getSample().get("addressLine1"))))
        .andExpect(jsonPath("$[0].sample.townName", is(rmCase0.getSample().get("townName"))))
        .andExpect(jsonPath("$[0].sample.postcode", is(rmCase0.getSample().get("postcode"))))
        .andExpect(jsonPath("$[1].caseId", is(rmCase1.getCaseId().toString())))
        .andExpect(
            jsonPath("$[1].sample.addressLine1", is(rmCase1.getSample().get("addressLine1"))))
        .andExpect(jsonPath("$[1].sample.townName", is(rmCase1.getSample().get("townName"))))
        .andExpect(jsonPath("$[1].sample.postcode", is(rmCase1.getSample().get("postcode"))));
  }

  /** Test not finding any matching cases */
  @Test
  public void caseNotFound() throws Exception {
    List<CaseDTO> emptyCaseResultsList = new ArrayList<>();
    when(caseService.findCasesBySampleAttribute("shoeSize", "14")).thenReturn(emptyCaseResultsList);

    mockMvc
        .perform(get("/cases/attribute/shoeSize/14"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.length()", is(0)));
  }

  //  @Test
  //  public void shouldFulfilBySms() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isOk());
  //    verify(caseService).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  //  }
  //
  //  @Test
  //  public void shouldFulfilBySmsWithNullClientIP() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    json.putNull("clientIP");
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isOk());
  //    verify(caseService).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  //  }
  //
  //  @Test
  //  public void shouldFulfilBySmsWithEmptyClientIP() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    json.put("clientIP", "");
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isOk());
  //    verify(caseService).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  //  }

  public void shouldRejectFulfilByPostWithIncorrectRequestBody() throws Exception {
    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/post";
    String json = "{ \"name\": \"Fred\" }";
    verifyRejectedPostFulfilmentRequest(json, url);
  }

  // ---

  //  @Test
  //  public void shouldRejectFulfilBySmsWhenPhoneNumberFailsRegex() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.put("telNo", "abc123");
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithMismatchedCaseIds() throws Exception {
  //    String url = "/cases/" + INCONSISTENT_CASEID + "/fulfilment/sms";
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithBadlyFormedCaseId() throws Exception {
  //    String url = "/cases/abc/fulfilment/sms";
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithBadlyFormedDate() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.put("dateTime", "2019:12:25 12:34:56");
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithMissingFulfilmentCodes() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.remove("fulfilmentCodes");
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithEmptyFulfilmentCodes() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.putArray("fulfilmentCodes");
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithAnEmptyFulfilmentCode() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.putArray("fulfilmentCodes").add("");
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithNullFulfilmentCode() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.putArray("fulfilmentCodes").addNull();
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }
  //
  //  @Test
  //  public void shouldAcceptFulfilBySmsWithMultipleValidFulfilmentCode() throws Exception {
  //    ObjectNode json = getSmsFulfilmentFixture();
  //    String url = "/cases/" + json.get("caseId").asText() + "/fulfilment/sms";
  //    json.putArray("fulfilmentCodes").add("A").add("B").add(StringUtils.repeat("C", 12));
  //    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isOk());
  //    verify(caseService).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  //  }
  //
  //  @Test
  //  public void shouldRejectFulfilBySmsWithIncorrectRequestBody() throws Exception {
  //    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/sms";
  //    String json = "{ \"name\": \"Fred\" }";
  //    verifyRejectedSmsFulfilmentRequest(json, url);
  //  }

  //  private ObjectNode getSmsFulfilmentFixture() {
  //    return FixtureHelper.loadClassObjectNode("SMSFulfilmentRequestDTO");
  //  }

  private ObjectNode getPostFulfilmentFixture() {
    return FixtureHelper.loadClassObjectNode("postal");
  }

  private ObjectNode getNewCaseEventFixture() {
    return FixtureHelper.loadClassObjectNode("NewCaseEventDTO");
  }

  //  private void verifyRejectedSmsFulfilmentRequest(ObjectNode json, String url) throws Exception
  // {
  //    verifyRejectedSmsFulfilmentRequest(json.toString(), url);
  //  }

  //  private void verifyRejectedSmsFulfilmentRequest(String json, String url) throws Exception {
  //    mockMvc.perform(postJson(url, json)).andExpect(status().isBadRequest());
  //    verify(caseService, never()).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  //  }

  private void verifyRejectedPostFulfilmentRequest(ObjectNode json, String url) throws Exception {
    verifyRejectedPostFulfilmentRequest(json.toString(), url);
  }

  private void verifyRejectedPostFulfilmentRequest(String json, String url) throws Exception {
    mockMvc.perform(postJson(url, json)).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PrintFulfilmentRequestDTO.class));
  }
}
