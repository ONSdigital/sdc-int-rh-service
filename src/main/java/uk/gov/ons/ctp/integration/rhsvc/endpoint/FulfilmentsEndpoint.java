package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.rhsvc.representation.OldProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FulfilmentsService;

/** The REST controller for the RH Fulfilment end points */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class FulfilmentsEndpoint implements CTPEndpoint {
  private FulfilmentsService fulfilmentsService;

  @Autowired
  public FulfilmentsEndpoint(final FulfilmentsService fulfilmentsService) {
    this.fulfilmentsService = fulfilmentsService;
  }

  /**
   * The GET end point to retrieve fulfilment details, ie product codes for case type, region and
   * delivery channel.
   *
   * <p>All request parameters are optional. If no parameters are specified then all products which
   * are applicable to RH are returned.
   *
   * @param caseType is an optional parameter to specify the case type, eg, 'HI' or 'HH'
   * @param region is an optional parameter to specify the region, eg, 'E' for England.
   * @param deliveryChannel is an optional parameter to specify the delivery channel, eg, 'POST'
   * @param individual is an optional parameter to specify whether this is a query about an
   *     individual or a household
   * @param productGroup is an optional parameter to specify the product group, eg 'UAC'
   * @return A list of matching products. The list will be empty if there are no matching products.
   * @throws CTPException if something went wrong.
   */
  @RequestMapping(value = "/fulfilments", method = RequestMethod.GET)
  public ResponseEntity<List<OldProductDTO>> getFulfilments(
      @RequestParam(required = false) CaseType caseType,
      @RequestParam(required = false) Region region,
      @RequestParam(required = false) DeliveryChannel deliveryChannel,
      @RequestParam(required = false) Boolean individual,
      @RequestParam(required = false) Product.ProductGroup productGroup)
      throws CTPException {

    log.info(
        "Entering GET getFulfilments",
        kv("requestParam.caseType", caseType),
        kv("requestParam.region", region),
        kv("requestParam.deliveryChannel", deliveryChannel),
        kv("requestParam.individual", individual),
        kv("requestParam.productGroup", productGroup));
    List<CaseType> caseTypes = caseType == null ? Collections.emptyList() : Arrays.asList(caseType);
    List<OldProductDTO> fulfilments =
        fulfilmentsService.getFulfilments(
            caseTypes, region, deliveryChannel, productGroup, individual);

    List<String> fulfilmentCodes =
        fulfilments.stream().map(OldProductDTO::getFulfilmentCode).collect(Collectors.toList());
    log.info(
        "Found fulfilment(s)", kv("size", fulfilments.size()), kv("fulfilments", fulfilmentCodes));

    log.debug(
        "Exit GET getFulfilments",
        kv("requestParam.caseType", caseType),
        kv("requestParam.productGroup", productGroup));

    return ResponseEntity.ok(fulfilments);
  }
}
