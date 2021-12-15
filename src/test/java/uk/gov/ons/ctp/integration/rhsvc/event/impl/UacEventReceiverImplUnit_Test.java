package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacPayload;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.QueueConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.UacRepository;

@ExtendWith(MockitoExtension.class)
public class UacEventReceiverImplUnit_Test {

  private UacRepository mockRespondentUacRepo;
  private EventFilter eventFilter;
  private UACEventReceiverImpl target;
  private UacEvent UacEventFixture;
  private UacUpdate uacFixture;
  private AppConfig appConfig = new AppConfig();

  @BeforeEach
  public void setUp() {
    target = new UACEventReceiverImpl();
    QueueConfig queueConfig = new QueueConfig();
    queueConfig.setQidFilterPrefixes(Stream.of("11", "12", "13", "14").collect(Collectors.toSet()));
    appConfig.setQueueConfig(queueConfig);
    ReflectionTestUtils.setField(target, "appConfig", appConfig);
    mockRespondentUacRepo = mock(UacRepository.class);
    eventFilter = mock(EventFilter.class);
    target.setRespondentUacRepo(mockRespondentUacRepo);
    target.setEventFilter(eventFilter);
  }

  @SneakyThrows
  private void prepareAndAcceptEvent(String qid, EventTopic topic) {
    // Construct UacEvent
    UacEventFixture = new UacEvent();
    UacPayload uacPayloadFixture = UacEventFixture.getPayload();
    uacFixture = uacPayloadFixture.getUacUpdate();
    uacFixture.setQid(qid);

    Header headerFixture = new Header();
    headerFixture.setTopic(topic);
    headerFixture.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    UacEventFixture.setHeader(headerFixture);

    // execution
    target.acceptUACEvent(UacEventFixture);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid) {
    acceptUacEvent(qid, EventTopic.UAC_UPDATE);
  }

  @SneakyThrows
  private void acceptUacEvent(String qid, EventTopic topic) {
    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(true);
    prepareAndAcceptEvent(qid, topic);
    verify(mockRespondentUacRepo).writeUAC(uacFixture);
  }

  @SneakyThrows
  private void filterUacEvent(String qid) {
    prepareAndAcceptEvent(qid, EventTopic.UAC_UPDATE);
    verify(mockRespondentUacRepo, never()).writeUAC(uacFixture);
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
    acceptUacEvent(RespondentHomeFixture.QID_01, EventTopic.UAC_UPDATE);
  }

  @Test
  public void shouldRejectUacWhenPrerequisiteEventsDoNotExistInFirestore() throws CTPException {
    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(false);
    prepareAndAcceptEvent(RespondentHomeFixture.QID_01, EventTopic.UAC_UPDATE);
    verify(mockRespondentUacRepo, never()).writeUAC(uacFixture);
  }

  @Test
  public void testExceptionThrown() throws CTPException {
    UacEvent uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
    uacEvent.getPayload().getUacUpdate().setQid(RespondentHomeFixture.QID_01);
    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(true);
    doThrow(new CTPException(CTPException.Fault.SYSTEM_ERROR))
        .when(mockRespondentUacRepo)
        .writeUAC(uacEvent.getPayload().getUacUpdate());

    CTPException thrown = assertThrows(CTPException.class, () -> target.acceptUACEvent(uacEvent));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, thrown.getFault());
    assertEquals("Non Specific Error", thrown.getMessage());
  }
}
