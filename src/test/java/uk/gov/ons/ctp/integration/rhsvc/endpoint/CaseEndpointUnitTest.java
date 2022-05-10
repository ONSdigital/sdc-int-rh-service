package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

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
import uk.gov.ons.ctp.integration.rhsvc.service.impl.CaseServiceImpl;

// ** Unit Tests on endpoint for Case resources */
@ExtendWith(MockitoExtension.class)
public class CaseEndpointUnitTest {

  private static final String UPRN = "123456";

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
}
