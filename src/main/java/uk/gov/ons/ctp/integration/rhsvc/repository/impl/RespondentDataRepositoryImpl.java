package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.CloudDataStore;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** A RespondentDataRepository implementation for CRUD operations on Respondent data entities */
@Slf4j
@Service
public class RespondentDataRepositoryImpl implements RespondentDataRepository {
  private RetryableCloudDataStore retryableCloudDataStore;

  // Cloud data store access for startup checks only
  @Autowired CloudDataStore nonRetryableCloudDataStore;

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloud-storage.case-schema-name}")
  private String caseSchemaName;

  @Value("${cloud-storage.uac-schema-name}")
  private String uacSchemaName;

  @Value("${cloud-storage.survey-schema-name}")
  private String surveySchemaName;

  @Value("${cloud-storage.collection-exercise-schema-name}")
  private String collectionExerciseSchemaName;

  String caseSchema;
  private String uacSchema;
  private String surveySchema;
  private String collectionExerciseSchema;

  private static final String[] SEARCH_BY_UPRN_PATH = new String[] {"sample", "uprn"};

  @PostConstruct
  public void init() {
    caseSchema = buildSchemaName(caseSchemaName);
    uacSchema = buildSchemaName(uacSchemaName);
    surveySchema = buildSchemaName(surveySchemaName);
    collectionExerciseSchema = buildSchemaName(collectionExerciseSchemaName);

    // Verify that Cloud Storage is working before consuming any events
    try {
      runCloudStartupCheck();
    } catch (Throwable e) {
      // There was some sort of failure with the cloud data storage.
      // Abort the process to prevent a half dead service consuming events that would shortly end
      // up on the DLQ
      log.error(
          "Failed cloud storage startup check. Unable to write to storage. Aborting service", e);
      System.exit(-1);
    }
  }

  private String buildSchemaName(String baseSchemaName) {
    return gcpProject + "-" + baseSchemaName.toLowerCase();
  }

  @Autowired
  public RespondentDataRepositoryImpl(RetryableCloudDataStore retryableCloudDataStore) {
    this.retryableCloudDataStore = retryableCloudDataStore;
  }

  /**
   * Stores a UAC object into the cloud data store.
   *
   * @param uac - object to be stored in the cloud
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    retryableCloudDataStore.storeObject(uacSchema, uac.getUacHash(), uac, uac.getCaseId());
  }

  /**
   * Read a UAC object from cloud.
   *
   * @param universalAccessCodeHash - the hash of the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<UAC> readUAC(final String universalAccessCodeHash) throws CTPException {
    return retryableCloudDataStore.retrieveObject(UAC.class, uacSchema, universalAccessCodeHash);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param caseUpdate - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
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
  @Override
  public Optional<CaseUpdate> readCaseUpdate(final String caseId) throws CTPException {
    return retryableCloudDataStore.retrieveObject(CaseUpdate.class, caseSchema, caseId);
  }

  /**
   * Read a Survey object from cloud.
   *
   * @param surveyId - the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<SurveyUpdate> readSurvey(final String surveyId) throws CTPException {
    return retryableCloudDataStore.retrieveObject(SurveyUpdate.class, surveySchema, surveyId);
  }

  /**
   * Write a Survey object into the cloud data store.
   *
   * @param surveyUpdate - is the survey to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeSurvey(final SurveyUpdate surveyUpdate) throws CTPException {
    String id = surveyUpdate.getSurveyId();
    retryableCloudDataStore.storeObject(surveySchema, id, surveyUpdate, id);
  }

  /**
   * Read a CollectionExercise object from cloud.
   *
   * @param collectionExerciseId - the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CollectionExercise> readCollectionExercise(final String collectionExerciseId)
      throws CTPException {
    return retryableCloudDataStore.retrieveObject(
        CollectionExercise.class, collectionExerciseSchema, collectionExerciseId);
  }

  /**
   * Write a CollectionExercise object into the cloud data store.
   *
   * @param collectionExercise - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeCollectionExercise(final CollectionExercise collectionExercise)
      throws CTPException {
    String id = collectionExercise.getCollectionExerciseId();
    retryableCloudDataStore.storeObject(collectionExerciseSchema, id, collectionExercise, id);
  }

  /**
   * Read case objects from cloud based on its uprn. Filter optionally whether the case is valid.
   *
   * @param uprn - is the uprn that the target case(s) must contain.
   * @param onlyValid - true if only valid cases to be returned; false if we don't care
   * @return - Optional containing 1 deserialised version of the stored object. If no matching cases
   *     are found then an empty Optional is returned.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CaseUpdate> readCaseUpdateByUprn(final String uprn, boolean onlyValid)
      throws CTPException {
    List<CaseUpdate> searchResults =
        retryableCloudDataStore.search(CaseUpdate.class, caseSchema, SEARCH_BY_UPRN_PATH, uprn);
    return filterValidCaseUpdateSearchResults(searchResults, onlyValid);
  }

  /**
   * Filter search results returning valid case
   *
   * @param searchResults - Search results found in dataStore by searching by uprn
   * @param onlyValid - true if only valid cases to be returned; false if we don't care
   * @return Optional of the resulting collection case or Empty
   */
  // TODO Used to filter on Non HI cases using CaseCreationDate to return the latest valid Non HI
  // case,
  // CaseCreatedDate may need to be added back if we receive duplicate cases
  private Optional<CaseUpdate> filterValidCaseUpdateSearchResults(
      final List<CaseUpdate> searchResults, boolean onlyValid) {
    return searchResults.stream().filter(c -> !onlyValid || !c.isInvalid()).findFirst();
  }

  private void runCloudStartupCheck() throws Throwable {
    // Find out if we are doing a cloud storage check on startup.
    // Default is to always to do the check unless disabled by an environment variable
    boolean checkCloudStorageOnStartup = true;
    String checkCloudStorageOnStartupStr = System.getenv("CHECK_CLOUD_STORAGE_ON_STARTUP");
    if (checkCloudStorageOnStartupStr != null
        && checkCloudStorageOnStartupStr.equalsIgnoreCase("false")) {
      checkCloudStorageOnStartup = false;
    }

    if (checkCloudStorageOnStartup) {
      // Test connectivity with cloud storage by writing a test object
      log.info("About to run cloud storage startup check");
      String startupCheckKey = writeCloudStartupCheckObject();
      log.info("Passed cloud storage startup check", kv("startupAuditId", startupCheckKey));
    } else {
      log.info("Skipping cloud storage startup check");
    }
  }

  /**
   * Confirms cloud datastore connection by writing an object.
   *
   * @return String containing the primary key for the datastore check.
   * @throws Exception - if a cloud exception was detected.
   */
  @Override
  public String writeCloudStartupCheckObject() throws Exception {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null) {
      hostname = "unknown-host";
    }

    // Create an object to write to the datastore.
    // To prevent any problems with multiple RH instances writing to the same record at
    // the same time, each one will contain a UUID to make it unique
    DatastoreStartupCheckData startupAuditData = new DatastoreStartupCheckData();
    String timestamp = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss").format(new Date());
    startupAuditData.setHostname(hostname);
    startupAuditData.setTimestamp(timestamp);

    // Attempt to write to datastore. Note that there are no retries on this.
    // We don't expect any contention on the collection so the write will either succeed or fail
    // (which will result in GCP restarting the service)
    String schemaName = gcpProject + "-" + "datastore-startup-check";
    String primaryKey = timestamp + "-" + hostname;
    nonRetryableCloudDataStore.storeObject(schemaName, primaryKey, startupAuditData);

    return primaryKey;
  }
}
