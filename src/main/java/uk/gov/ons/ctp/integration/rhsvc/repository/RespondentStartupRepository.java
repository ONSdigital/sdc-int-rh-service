package uk.gov.ons.ctp.integration.rhsvc.repository;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.FirestoreDataStore;

/**
 * This class verifies that RH can get a Firestore connection, by writing an object to the startup
 * collection
 */
@Slf4j
@Service
public class RespondentStartupRepository {

  // Cloud data store access for startup checks only
  @Autowired FirestoreDataStore nonRetryableCloudDataStore;

  @Value("${spring.cloud.gcp.firestore.project-id}")
  private String gcpProject;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DatastoreStartupCheckData {
    private String timestamp;
    private String hostname;
  }

  @PostConstruct
  public void init() {
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
