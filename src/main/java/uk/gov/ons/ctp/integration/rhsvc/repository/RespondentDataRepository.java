package uk.gov.ons.ctp.integration.rhsvc.repository;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UAC;

/** Repository for Respondent Data */
public interface RespondentDataRepository {

  void writeUAC(UAC uac) throws CTPException;

  void writeCollectionCase(CollectionCase collectionCase) throws CTPException;

  void writeSurvey(SurveyUpdate surveyUpdate) throws CTPException;

  void writeCollectionExercise(CollectionExercise collectionExercise) throws CTPException;

  Optional<UAC> readUAC(String universalAccessCode) throws CTPException;

  Optional<CollectionCase> readCollectionCase(String caseId) throws CTPException;

  Optional<SurveyUpdate> readSurvey(String caseId) throws CTPException;

  Optional<CollectionExercise> readCollectionExercise(String caseId) throws CTPException;

  Optional<CollectionCase> readNonHILatestCollectionCaseByUprn(String uprn, boolean onlyValid)
      throws CTPException;

  String writeCloudStartupCheckObject() throws Exception;
}
