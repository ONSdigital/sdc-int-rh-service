package uk.gov.ons.ctp.integration.rhsvc.repository;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;

/** Repository for Respondent Data */
public interface RespondentDataRepository {

  void writeUAC(UacUpdate uac) throws CTPException;

  void writeCaseUpdate(CaseUpdate caseUpdate) throws CTPException;

  void writeSurvey(SurveyUpdate surveyUpdate) throws CTPException;

  void writeCollectionExercise(CollectionExercise collectionExercise) throws CTPException;

  Optional<UacUpdate> readUAC(String universalAccessCode) throws CTPException;

  Optional<CaseUpdate> readCaseUpdate(String caseId) throws CTPException;

  Optional<SurveyUpdate> readSurvey(String surveyId) throws CTPException;

  Optional<CollectionExercise> readCollectionExercise(String collectionExerciseId)
      throws CTPException;

  Optional<CaseUpdate> readCaseUpdateByUprn(String uprn, boolean onlyValid) throws CTPException;

  String writeCloudStartupCheckObject() throws Exception;
}
