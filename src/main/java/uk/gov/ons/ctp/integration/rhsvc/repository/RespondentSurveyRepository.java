package uk.gov.ons.ctp.integration.rhsvc.repository;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;

/** A Repository implementation for CRUD operations on Survey data entities */
@Service
public class RespondentSurveyRepository {

  private RetryableCloudDataStore retryableCloudDataStore;

  @Value("${spring.cloud.gcp.firestore.project-id}")
  private String gcpProject;

  @Value("${cloud-storage.survey-schema-name}")
  private String surveySchemaName;

  private String surveySchema;

  @PostConstruct
  public void init() {
    surveySchema = gcpProject + "-" + surveySchemaName.toLowerCase();
  }

  @Autowired
  public RespondentSurveyRepository(RetryableCloudDataStore retryableCloudDataStore) {
    this.retryableCloudDataStore = retryableCloudDataStore;
  }

  /**
   * Read a Survey object from cloud.
   *
   * @param surveyId - the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  public Optional<SurveyUpdate> readSurvey(final String surveyId) throws CTPException {
    return retryableCloudDataStore.retrieveObject(SurveyUpdate.class, surveySchema, surveyId);
  }

  /**
   * Write a Survey object into the cloud data store.
   *
   * @param surveyUpdate - is the survey to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  public void writeSurvey(final SurveyUpdate surveyUpdate) throws CTPException {
    String id = surveyUpdate.getSurveyId();
    retryableCloudDataStore.storeObject(surveySchema, id, surveyUpdate, id);
  }

  /**
   * List all of the surveys.
   *
   * <p>Assumes that this list will never be so large as to be unwieldy.
   *
   * @return list of all the surveyUpdate objects
   * @throws CTPException - if a cloud exception was detected.
   */
  public List<SurveyUpdate> listSurveys() throws CTPException {
    return retryableCloudDataStore.list(SurveyUpdate.class, surveySchema);
  }
}
