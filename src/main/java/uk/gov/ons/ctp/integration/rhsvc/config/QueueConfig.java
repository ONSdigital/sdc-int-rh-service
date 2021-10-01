package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class QueueConfig {
  private String caseSubscription;
  private String uacSubscription;
  private String surveySubscription;
  private String collectionExerciseSubscription;
  private Set<String> qidFilterPrefixes;
}
