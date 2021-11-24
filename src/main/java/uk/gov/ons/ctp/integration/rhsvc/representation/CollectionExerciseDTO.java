package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class CollectionExerciseDTO {

  private UUID collectionExerciseId;
  private UUID surveyId;
  private String name;
  private String reference;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int numberOfWaves;
  private int waveLength;
  private int cohorts;
  private int cohortSchedule;
}
