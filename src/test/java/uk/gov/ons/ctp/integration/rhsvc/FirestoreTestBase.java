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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
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
  private static final String GCP_PROJECT = "rh-testcontainers";
  protected static final String CASE_SCHEMA = GCP_PROJECT + "-case";
  protected static final String UAC_SCHEMA = GCP_PROJECT + "-uac";
  protected static final String COLLEX_SCHEMA = GCP_PROJECT + "-collection_exercise";
  protected static final String SURVEY_SCHEMA = GCP_PROJECT + "-survey";

  @Autowired protected TestCloudDataStore testDataStore;

  private static TestFirestoreProvider provider;

  @Container
  private static final CustomFirestoreEmulatorContainer firestoreEmulator =
      new CustomFirestoreEmulatorContainer(
          DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:317.0.0-emulators"));

  @BeforeAll
  public static void classSetup() {
    log.info("Initialising test firestoreProvider");
    if (provider == null) {
      provider = new TestFirestoreProvider();
    }
    provider.init(GCP_PROJECT, firestoreEmulator);
  }

  @TestConfiguration
  static class EmulatorConfiguration {
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

  @Slf4j
  public static class TestFirestoreProvider implements FirestoreProvider {
    private Firestore firestore;

    public TestFirestoreProvider() {
      log.info("Creating new TestFirestoreProvider");
      System.out.println("CREATING NEW TestFirestoreProvider");
    }

    public TestFirestoreProvider init(
        String gcpProject, CustomFirestoreEmulatorContainer emulator) {
      log.info("Creating emulator firestore instance with projectId {}", gcpProject);
      System.out.println(
          "CREATING EMULATOR FIRESTORE: "
              + gcpProject
              + " on endpoint: "
              + emulator.getEmulatorEndpoint());
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

  @Slf4j
  public static class CustomFirestoreEmulatorContainer
      extends GenericContainer<CustomFirestoreEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk");

    private static final String CMD =
        "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";
    private static final int PORT = 8080;

    public CustomFirestoreEmulatorContainer(final DockerImageName dockerImageName) {
      super(dockerImageName);

      System.out.println("SYSOUT STARTING FIRESTORE EMULATOR !");

      log.info("STARTING FIRESTORE EMULATOR");

      dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

      withExposedPorts(PORT);
      setWaitStrategy(new LogMessageWaitStrategy().withRegEx("(?s).*running.*$"));
      withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     *     reachable from the test host machine. Directly usable as a parameter to the
     *     com.google.cloud.ServiceOptions.Builder#setHost(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
      return getContainerIpAddress() + ":" + getMappedPort(8080);
    }
  }
}
