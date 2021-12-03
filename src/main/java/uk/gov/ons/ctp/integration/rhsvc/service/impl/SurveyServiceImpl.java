package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.ProductGroup;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyService;

@Service
public class SurveyServiceImpl implements SurveyService {
  private RespondentDataRepository dataRepo;
  private RHSvcBeanMapper mapper;

  public SurveyServiceImpl(RespondentDataRepository dataRepo, RHSvcBeanMapper mapper) {
    this.dataRepo = dataRepo;
    this.mapper = mapper;
  }

  @Override
  public List<SurveyDTO> listSurveys() throws CTPException {
    List<SurveyUpdate> surveys = dataRepo.listSurveys();
    var dtos = surveys.stream().map(this::from).collect(toList());
    return dtos;
  }

  @Override
  public SurveyDTO survey(UUID surveyId) throws CTPException {
    SurveyUpdate surveyUpdate =
        dataRepo
            .readSurvey(surveyId.toString())
            .orElseThrow(
                () ->
                    new CTPException(
                        CTPException.Fault.RESOURCE_NOT_FOUND,
                        "Survey " + surveyId + " Not Found"));

    SurveyDTO dto = from(surveyUpdate);
    return dto;
  }

  private SurveyDTO from(SurveyUpdate surveyUpdate) {
    SurveyDTO dto = mapper.map(surveyUpdate, SurveyDTO.class);
    dto.setSurveyType(surveyUpdate.surveyType());

    List<ProductDTO> products = new ArrayList<>();

    mapFulfilments(DeliveryChannel.EMAIL, surveyUpdate.getAllowedEmailFulfilments(), products);
    mapFulfilments(DeliveryChannel.SMS, surveyUpdate.getAllowedSmsFulfilments(), products);
    mapFulfilments(DeliveryChannel.POST, surveyUpdate.getAllowedPrintFulfilments(), products);

    dto.setAllowedFulfilments(products);
    return dto;
  }

  private void mapFulfilments(
      DeliveryChannel deliveryChannel,
      List<SurveyFulfilment> fulfilments,
      List<ProductDTO> products) {
    if (fulfilments == null) {
      return;
    }
    for (SurveyFulfilment f : fulfilments) {
      ProductDTO product = mapper.map(f, ProductDTO.class);
      product.setDeliveryChannel(deliveryChannel);
      product.setProductGroup(ProductGroup.UAC);
      products.add(product);
    }
  }
}
