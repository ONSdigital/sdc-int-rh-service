package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.UUID;

import lombok.Data;

@Data
public class Sis {
  private String collectionExerciseId;
  
  public UUID getCollectionExerciseIdAsUUID() {
    return UUID.fromString(collectionExerciseId);
  }
}
