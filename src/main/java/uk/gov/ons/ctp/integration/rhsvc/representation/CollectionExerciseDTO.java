package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Date;
import lombok.Data;

@Data
public class CollectionExerciseDTO {

  private String collectionExerciseId;
  private String surveyId;
  private String name;
  private String reference;
  private Date startDate;
  private Date endDate;
  private Integer numberOfWaves;
  private Integer waveLength;
  private Integer cohorts;
  private Integer cohortSchedule;
}
