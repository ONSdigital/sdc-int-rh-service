package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class QueueConfig {
  private String caseSubscription;
  private String caseTopic;
  private String uacSubscription;
  private String uacTopic;
  private String surveySubscription;
  private String surveyTopic;
  private String collectionExerciseSubscription;
  private String ollectionExerciseTopic;
  private Set<String> qidFilterPrefixes;
}
