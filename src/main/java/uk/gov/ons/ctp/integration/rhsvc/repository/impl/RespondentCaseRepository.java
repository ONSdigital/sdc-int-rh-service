package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;

/** A Repository implementation for CRUD operations on Case data entities */
@Service
public class RespondentCaseRepository {
  private RetryableCloudDataStore retryableCloudDataStore;

  @Value("${spring.cloud.gcp.firestore.project-id}")
  private String gcpProject;

  @Value("${cloud-storage.case-schema-name}")
  private String caseSchemaName;

  String caseSchema;

  private static final String SEARCH_SAMPLE_PATH = "sample";

  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
  }

  @Autowired
  public RespondentCaseRepository(RetryableCloudDataStore retryableCloudDataStore) {
    this.retryableCloudDataStore = retryableCloudDataStore;
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param caseUpdate - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  public void writeCaseUpdate(final CaseUpdate caseUpdate) throws CTPException {
    String id = caseUpdate.getCaseId();
    retryableCloudDataStore.storeObject(caseSchema, id, caseUpdate, id);
  }

  /**
   * Read a Case object from cloud.
   *
   * @param caseId - the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  public Optional<CaseUpdate> readCaseUpdate(final String caseId) throws CTPException {
    return retryableCloudDataStore.retrieveObject(CaseUpdate.class, caseSchema, caseId);
  }

  /**
   * Read case objects from cloud based on its uprn. Filter optionally whether the case is valid.
   *
   * @param searchAttributeName - is the name of the field in the sample data to search by.
   * @param searchValue - is the value that target case(s) must contain.
   * @param onlyValid - true if only valid cases to be returned; false if we don't care
   * @return - a List containing the deserialised version of all matching stored objects. If no
   *     matching cases are found then the list will be empty.
   * @throws CTPException - if a cloud exception was detected.
   */
  public List<CaseUpdate> readCaseUpdateBySampleAttribute(
      final String searchAttributeName, final String searchValue, boolean onlyValid)
      throws CTPException {

    String[] searchPath = {SEARCH_SAMPLE_PATH, searchAttributeName};

    List<CaseUpdate> searchResults =
        retryableCloudDataStore.search(CaseUpdate.class, caseSchema, searchPath, searchValue);
    return filterValidCaseUpdateSearchResults(searchResults, onlyValid);
  }

  /**
   * Filter search results returning valid case
   *
   * @param searchResults - Search results found in dataStore by searching by sample attribute
   *     name/value.
   * @param onlyValid - true if only valid cases to be returned; false if we don't care
   * @return List of the cases after filtering. List will be empty if there are no resulting cases.
   */
  // TODO Used to filter on Non HI cases using CaseCreationDate to return the latest valid Non HI
  // case,
  // CaseCreatedDate may need to be added back if we receive duplicate cases
  private List<CaseUpdate> filterValidCaseUpdateSearchResults(
      final List<CaseUpdate> searchResults, boolean onlyValid) {
    return searchResults.stream()
        .filter(c -> !onlyValid || !c.isInvalid())
        .collect(Collectors.toList());
  }
}
