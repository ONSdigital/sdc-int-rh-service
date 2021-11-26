package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchedDTO;

/** Service responsible for EqLaunched requests */
public interface EqLaunchedService {

  public void eqLaunched(EqLaunchedDTO eqLaunchedDTO) throws CTPException;
}
