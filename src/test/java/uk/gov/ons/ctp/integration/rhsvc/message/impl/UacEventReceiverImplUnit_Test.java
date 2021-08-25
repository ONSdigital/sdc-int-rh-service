package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacPayload;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.QueueConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

@ExtendWith(MockitoExtension.class)
public class UacEventReceiverImplUnit_Test {

  private RespondentDataRepository mockRespondentDataRepo;
  private UACEventReceiverImpl target;
  private UacEvent UacEventFixture;
  private UAC uacFixture;
  private AppConfig appConfig = new AppConfig();

  @BeforeEach
  public void setUp() {
    target = new UACEventReceiverImpl();
    QueueConfig queueConfig = new QueueConfig();
    queueConfig.setQidFilterPrefixes(Stream.of("11", "12", "13", "14").collect(Collectors.toSet()));
    appConfig.setQueueConfig(queueConfig);
    ReflectionTestUtils.setField(target, "appConfig", appConfig);
    mockRespondentDataRepo = mock(RespondentDataRepositoryImpl.class);
    target.setRespondentDataRepo(mockRespondentDataRepo);
  }

  @SneakyThrows
  private void prepareAndAcceptEvent(String qid, EventType type) {
    // Construct UacEvent
    UacEventFixture = new UacEvent();
    UacPayload uacPayloadFixture = UacEventFixture.getPayload();
    uacFixture = uacPayloadFixture.getUac();
    uacFixture.setQuestionnaireId(qid);

    Header headerFixture = new Header();
    headerFixture.setType(type);
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    UacEventFixture.setEvent(headerFixture);

    // execution
    target.acceptUACEvent(UacEventFixture);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid) {
    acceptUacEvent(qid, EventType.UAC_UPDATE);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid, EventType type) {
    prepareAndAcceptEvent(qid, type);
    verify(mockRespondentDataRepo).writeUAC(uacFixture);
  }

  @SneakyThrows
  private void filterUacEvent(String qid) {
    prepareAndAcceptEvent(qid, EventType.UAC_UPDATE);
    verify(mockRespondentDataRepo, never()).writeUAC(uacFixture);
  }

  @Test
  public void shouldAcceptUacEventPrefix01() {
    acceptUacEvent(RespondentHomeFixture.QID_01);
  }

  @Test
  public void shouldAcceptUacEventPrefix21() {
    acceptUacEvent(RespondentHomeFixture.QID_21);
  }

  @Test
  public void shouldAcceptUacEventPrefix31() {
    acceptUacEvent(RespondentHomeFixture.QID_31);
  }

  @Test
  public void shouldFilterUacEventPrefix11() {
    filterUacEvent(RespondentHomeFixture.QID_11);
  }

  @Test
  public void shouldFilterUacEventPrefix12() {
    filterUacEvent(RespondentHomeFixture.QID_12);
  }

  @Test
  public void shouldFilterUacEventPrefix13() {
    filterUacEvent(RespondentHomeFixture.QID_13);
  }

  @Test
  public void shouldFilterUacEventPrefix14() {
    filterUacEvent(RespondentHomeFixture.QID_14);
  }

  @Test
  public void shouldAcceptUacCreatedEvent() {
    acceptUacEvent(RespondentHomeFixture.QID_01, EventType.UAC_UPDATE);
  }
}
