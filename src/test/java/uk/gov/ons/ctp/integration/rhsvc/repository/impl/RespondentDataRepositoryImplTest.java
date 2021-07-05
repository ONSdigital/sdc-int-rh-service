package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

@ExtendWith(MockitoExtension.class)
public class RespondentDataRepositoryImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");
  private static final String UPRN_STRING = Long.toString(UPRN.getValue());

  @Spy private RetryableCloudDataStore mockCloudDataStore;

  @InjectMocks private RespondentDataRepositoryImpl target;

  private List<CollectionCase> collectionCase;
  private final String[] searchByUprnPath = new String[] {"address", "uprn"};

  /** Setup tests */
  @BeforeEach
  public void setUp() throws Exception {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    ReflectionTestUtils.setField(target, "caseSchema", "SCHEMA");
  }

  /** Returns Empty Optional where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {

    final List<CollectionCase> emptyList = new ArrayList<>();
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(emptyList);

    assertEquals(
        Optional.empty(),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects Empty Optional");
  }

  /** Returns Empty Optional where no valid caseType cases are returned from repository */
  @Test
  public void getInvalidCaseTypeCasesByUPRNOnly() throws Exception {

    collectionCase.forEach(cc -> cc.setCaseType("HI"));
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.empty(),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects Empty Optional");
  }

  /** Returns Empty Optional where no valid Address cases are returned from repository */
  @Test
  public void getInvalidAddressedCasesByUPRNOnly() throws Exception {

    collectionCase.forEach(cc -> cc.setAddressInvalid(Boolean.TRUE));
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.empty(),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects Empty Optional");
  }

  /** Test retrieves latest case when all valid HH */
  @Test
  public void getLatestCaseByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);

    collectionCase.forEach(cc -> cc.setCaseType("HH"));
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(1).setCreatedDateTime(latest); // EXPECTED
    collectionCase.get(2).setCreatedDateTime(earliest);
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.of(collectionCase.get(1)),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects Item with Latest Date");
  }

  /** Test retrieves latest valid case when actual latest date is an HI case */
  @Test
  public void getLatestCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH"); // EXPECTED
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.of(collectionCase.get(0)),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects HH Item With Latest Date");
  }

  /** Test retrieves latest Address valid case when actual latest date is an HI case */
  @Test
  public void getLatestAddressValidCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH");
    collectionCase.get(0).setAddressInvalid(Boolean.TRUE); // INVALID
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID / EXPECTED
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.of(collectionCase.get(2)),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, true),
        "Expects Latest Item With Valid Address");
  }

  /** Test retrieves latest invalid Address case when actual latest date is an HI case */
  @Test
  public void getLatestAddressCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH");
    collectionCase.get(0).setAddressInvalid(Boolean.TRUE); // INVALID , but EXPECTED
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID
    when(mockCloudDataStore.search(
            CollectionCase.class, target.caseSchema, searchByUprnPath, UPRN_STRING))
        .thenReturn(collectionCase);

    assertEquals(
        Optional.of(collectionCase.get(0)),
        target.readNonHILatestCollectionCaseByUprn(UPRN_STRING, false),
        "Expects Latest Item With non HI Address");
  }
}
