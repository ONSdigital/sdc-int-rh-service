package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;

/** Implementation to deal with Case data */
@Slf4j
@Service
// I really want to rename this, but stuff blows up real bad, probably why it wasn't renamed before
// Try again when dependency on 'common' is gone
public class CaseServiceImpl {
  @Autowired private AppConfig appConfig;
  @Autowired private CaseRepository dataRepo;
  @Autowired private SurveyRepository surveyRepository;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private RateLimiterClient rateLimiterClient;

  private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public List<CaseDTO> findCasesBySampleAttribute(
      final String attributeKey, final String attributeValue) throws CTPException {

    List<CaseUpdate> foundCases =
        dataRepo.findCaseUpdatesBySampleAttribute(attributeKey, attributeValue, true);
    log.debug(
        "Search for cases by attribute value",
        kv("numberFoundCase", foundCases.size()),
        kv("searchAttributeName", attributeKey),
        kv("searchValue", attributeValue));
    return mapperFacade.mapAsList(foundCases, CaseDTO.class);
  }
}
