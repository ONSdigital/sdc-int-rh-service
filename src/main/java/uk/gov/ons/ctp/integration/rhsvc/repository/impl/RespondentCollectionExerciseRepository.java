package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;

/** A repository implementation for CRUD operations on CollectionExercise data entities */
@Service
public class RespondentCollectionExerciseRepository {
  private RetryableCloudDataStore retryableCloudDataStore;

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloud-storage.collection-exercise-schema-name}")
  private String collectionExerciseSchemaName;

  private String collectionExerciseSchema;

  @PostConstruct
  public void init() {
    collectionExerciseSchema = gcpProject + "-" + collectionExerciseSchemaName.toLowerCase();
  }

  @Autowired
  public RespondentCollectionExerciseRepository(RetryableCloudDataStore retryableCloudDataStore) {
    this.retryableCloudDataStore = retryableCloudDataStore;
  }

  /**
   * Read a CollectionExercise object from cloud.
   *
   * @param collectionExerciseId - the unique id of the object stored
   * @return - deserialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
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
  public void writeCollectionExercise(final CollectionExercise collectionExercise)
      throws CTPException {
    String id = collectionExercise.getCollectionExerciseId();
    retryableCloudDataStore.storeObject(collectionExerciseSchema, id, collectionExercise, id);
  }
}
