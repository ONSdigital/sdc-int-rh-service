package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

//@Service
public class GCSDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(GCSDataStore.class);
  private static final String EUROPE_WEST_2 = "europe-west2";
//PMB  private Storage storage = StorageOptions.getDefaultInstance().getService();

  @PostConstruct
  public void foo() {
    log.info("Now in storeObject method in GCSDataStore class");
  }

  /**
   * Write object in Cloud Storage for UAC details inside specified bucket
   *
   * @param bucket - represents the bucket where the object will be stored
   * @param key - represents the unique object identifier in the bucket for the object stored
   * @param value - represents the string value representation of the object to be stored
   */
  @Override
  public void storeObject(final String bucket, final String key, final String value)
  {
    //PMB      throws StorageException {

    log.info("Now in storeObject method in GCSDataStore class. " + System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

//PMB    try {
//      log.with(bucket).info("Now attempting to create the bucket");
//      createBucket(bucket, storage);
//      log.with(bucket).info("Bucket created successfully");
//    } catch (StorageException se) {
//      log.sagwith(se.getMese()).info("ERROR");
//    }
//
//    saveObjectToCloud(bucket, key, value, storage);
  }

  /**
   * Read object in Cloud Storage for Case details inside specified bucket
   *
   * @param bucket - represents the bucket where the object will be stored
   * @param key - represents the unique object identifier in the bucket for the object stored
   * @return - JSON string representation of the object retrieved
   */
  @Override
  public Optional<String> retrieveObject(final String bucket, final String key) {
    log.info("Now in the retrieveObject method in class GCSDataStore.");
    if (null == bucket || bucket.length() == 0) {
      log.with(bucket).info("Bucket name was not set for object retrieval");
      return Optional.empty();
    }
    if (null == key || key.length() == 0) {
      log.with(key).info("Key was not set for object retrieval");
      return Optional.empty();
    }
//PMB    BlobId blobId = BlobId.of(bucket, key);
//    Blob blob = storage.get(blobId);
//    if (getObjectFromCloud(bucket, key, blob)) {
//      return Optional.empty();
//    }
//
//    String value = new String(blob.getContent());
//    log.with(blobId).debug("Found BLOB: " + value);
//    return Optional.of(value);
    return null;  // PMB
  }

  @Override
  public void deleteObject(final String schema, final String key) {
    log.with(schema).with(key).info("Now in the deleteObject method in class GCSDataStore.");
//PMB    BlobId blobId = BlobId.of(bucket, key);
//    storage.delete(blobId);
//    log.with(blobId).info("Deleted item from bucket");
  }

//  private void saveObjectToCloud(String bucket, String key, String value, Storage storage)
//  throws StorageException {
//  
//    log.with(bucket).with(key).info("Now saving the object to the cloud");
//PMB    BlobId blobId = BlobId.of(bucket, key);
//    BlobInfo.Builder builder = BlobInfo.newBuilder(blobId);
//    BlobInfo blobInfo = builder.setContentType("text/plain").build();
//    storage.create(blobInfo, value.getBytes());
//    log.with(key).debug("Blob has been created in cloud storage");
//  }

//PMB  private void createBucket(String bucket, Storage storage) {
//    storage.create(
//        BucketInfo.newBuilder(bucket)
//            // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
//            .setStorageClass(StorageClass.REGIONAL)
//            // As John mentioned, I used Europe west 2 - location where data will be held
//            // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
//            .setLocation(EUROPE_WEST_2)
//            .build());
//  }

//PMB  private boolean getObjectFromCloud(String bucket, String key, Blob blob) {
//    if (null == blob) {
//      log.with(key)
//          .debug("Object could not be retrieved within cloud in bucket = <" + bucket + ">");
//      return true;
//    }
//    return false;
//  }
}
