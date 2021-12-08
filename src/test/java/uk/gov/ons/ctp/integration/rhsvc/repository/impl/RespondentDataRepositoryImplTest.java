package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;

@ExtendWith(MockitoExtension.class)
public class RespondentDataRepositoryImplTest {

  private static final String UPRN = "123456";

  @Spy private RetryableCloudDataStore mockCloudDataStore;

  @InjectMocks private RespondentDataRepositoryImpl target;

  private List<CaseUpdate> collectionCase;
  private final String[] searchByUprnPath = new String[] {"sample", "uprn"};

  /** Setup tests */
  @BeforeEach
  public void setUp() throws Exception {
    this.collectionCase = FixtureHelper.loadClassFixtures(CaseUpdate[].class);
    ReflectionTestUtils.setField(target, "caseSchema", "SCHEMA");
  }

  /** Returns Empty Optional where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {

    final List<CaseUpdate> emptyList = new ArrayList<>();
    when(mockCloudDataStore.search(
            CaseUpdate.class, target.caseSchema, searchByUprnPath, UPRN))
        .thenReturn(emptyList);

    assertEquals(
        Optional.empty(), target.readCaseUpdateBySampleAttribute("uprn", UPRN, true), "Expects Empty Optional");
  }

  /** Returns Empty Optional where no valid Address cases are returned from repository */
  @Test
  public void getInvalidCasesByUPRNOnly() throws Exception {

    collectionCase.forEach(cc -> cc.setInvalid(Boolean.TRUE));
    when(mockCloudDataStore.search(
            CaseUpdate.class, target.caseSchema, searchByUprnPath, UPRN))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.empty(), target.readCaseUpdateBySampleAttribute("uprn", UPRN, true), "Expects Empty Optional");
  }

  /** Test retrieves invalid Address case */
  @Test
  public void getInvalidCaseByUPRNOnly() throws Exception {

    collectionCase.get(0).setInvalid(Boolean.TRUE);
    when(mockCloudDataStore.search(
            CaseUpdate.class, target.caseSchema, searchByUprnPath, UPRN))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.of(collectionCase.get(0)),
        target.readCaseUpdateBySampleAttribute("uprn", UPRN, false),
        "Expects Latest Item With non HI Address");
  }
}
