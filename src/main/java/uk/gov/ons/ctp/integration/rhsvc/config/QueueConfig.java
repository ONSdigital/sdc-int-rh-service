package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class QueueConfig {
  private String caseSubscription;
  private String caseSubscriptionDLQ;
  private String caseTopic;
  private String uacSubscription;
  private String uacSubscriptionDLQ;
  private String uacTopic;
  private String responseAuthenticationTopic;
  private Set<String> qidFilterPrefixes;
}
