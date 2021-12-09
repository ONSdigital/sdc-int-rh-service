package uk.gov.ons.ctp.integration.rhsvc;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.ons.ctp.common.cloud.FirestoreProvider;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.firestore.TestCloudDataStore;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;

/** Base class for Firestore integration tests using TestContainers. */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test-containers-firestore")
@Testcontainers
@MockBean({EventPublisher.class, PubSubTemplate.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "uacEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
@Tag("firestore")
@Slf4j
public abstract class FirestoreTestBase {
  private static final String EMULATOR_IMG =
      "gcr.io/google.com/cloudsdktool/cloud-sdk:366.0.0-emulators";

  /**
   * This value must be kept in sync with spring.cloud.gcp.firestore.project-id value in the
   * application yaml files. It is hard-coded here in order to be picked up statically before the
   * application context gets created.
   */
  private static final String GCP_PROJECT = "rh-testcontainers";

  protected static final String CASE_SCHEMA = GCP_PROJECT + "-case";
  protected static final String UAC_SCHEMA = GCP_PROJECT + "-uac";
  protected static final String COLLEX_SCHEMA = GCP_PROJECT + "-collection_exercise";
  protected static final String SURVEY_SCHEMA = GCP_PROJECT + "-survey";

  @Autowired protected TestCloudDataStore testDataStore;

  private static TestFirestoreProvider provider;

  @Container
  private static final FirestoreEmulatorContainer firestoreEmulator =
      new FirestoreEmulatorContainer(DockerImageName.parse(EMULATOR_IMG));

  /**
   * The dockerised emulator will change its endpoint for each new test class, so therefore
   * firestore must be reconfigured for each test class that runs.
   */
  @BeforeAll
  public static void classSetup() {
    log.info(
        "Initialising test firestoreProvider against emulator: {}",
        firestoreEmulator.getEmulatorEndpoint());
    if (provider == null) {
      provider = new TestFirestoreProvider();
    }
    provider.init(GCP_PROJECT, firestoreEmulator);
  }

  /**
   * Configure any firestore provider in our test environment to use the emulator. This
   * FirestoreProvider bean will be picked up by everywhere using firestore and override the usual
   * implementation.
   */
  @TestConfiguration
  static class FirestoreConfiguration {
    @Bean
    @Primary
    FirestoreProvider firestoreProvider() {
      return provider;
    }
  }

  protected void deleteAllCollections() {
    testDataStore.deleteCollection(SURVEY_SCHEMA);
    testDataStore.deleteCollection(COLLEX_SCHEMA);
    testDataStore.deleteCollection(UAC_SCHEMA);
    testDataStore.deleteCollection(CASE_SCHEMA);
  }

  /** An implementation of FirestoreProvider that uses the emulator. */
  public static class TestFirestoreProvider implements FirestoreProvider {
    private Firestore firestore;

    public TestFirestoreProvider init(String gcpProject, FirestoreEmulatorContainer emulator) {
      firestore =
          FirestoreOptions.newBuilder()
              .setProjectId(gcpProject)
              .setHost(emulator.getEmulatorEndpoint())
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService();
      return this;
    }

    public Firestore get() {
      return firestore;
    }
  }
}
