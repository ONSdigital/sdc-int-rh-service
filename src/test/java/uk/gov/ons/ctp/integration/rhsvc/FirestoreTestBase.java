package uk.gov.ons.ctp.integration.rhsvc;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
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
public abstract class FirestoreTestBase {
  private static final String GCP_PROJECT = "sdc-rh-test";
  protected static final String CASE_SCHEMA = GCP_PROJECT + "-case";
  protected static final String UAC_SCHEMA = GCP_PROJECT + "-uac";
  protected static final String COLLEX_SCHEMA = GCP_PROJECT + "-collection_exercise";
  protected static final String SURVEY_SCHEMA = GCP_PROJECT + "-survey";

  @Autowired protected TestCloudDataStore dataStore;

  @Container
  private static final FirestoreEmulatorContainer firestoreEmulator =
      new FirestoreEmulatorContainer(
          DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:317.0.0-emulators"));

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.gcp.firestore.host-port", firestoreEmulator::getEmulatorEndpoint);
  }

  @TestConfiguration
  static class EmulatorConfiguration {
    // By default, autoconfiguration will initialize application default credentials.
    // For testing purposes, don't use any credentials. Bootstrap w/ NoCredentialsProvider.
    @Bean
    CredentialsProvider googleCredentials() {
      return NoCredentialsProvider.create();
    }
  }

  protected void deleteAllCollections() {
    dataStore.deleteCollection(SURVEY_SCHEMA);
    dataStore.deleteCollection(COLLEX_SCHEMA);
    dataStore.deleteCollection(UAC_SCHEMA);
    dataStore.deleteCollection(CASE_SCHEMA);
  }
}
