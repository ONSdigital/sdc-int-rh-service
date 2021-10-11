package uk.gov.ons.ctp.integration.rhsvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.common.event.model.NewCaseSample;
import uk.gov.ons.ctp.common.event.model.NewCaseSampleSensitive;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseSampleDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseSampleSensitiveDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

/** The bean mapper that maps to/from DTOs and repository entity types. */
@Component
public class RHSvcBeanMapper extends ConfigurableMapper {

  /**
   * Setup the mapper for all of our beans.
   *
   * @param factory the factory to which we add our mappings
   */
  protected final void configure(final MapperFactory factory) {

    ConverterFactory converterFactory = factory.getConverterFactory();
    converterFactory.registerConverter(new StringToUUIDConverter());
    converterFactory.registerConverter(new StringToUPRNConverter());
    converterFactory.registerConverter(new EstabTypeConverter());

    factory.classMap(CaseUpdate.class, CaseDTO.class).byDefault().register();

    factory
        .classMap(CaseUpdate.class, UniqueAccessCodeDTO.class)
        .field("sample.region", "region")
        .field("sample.uprn", "address.uprn")
        .field("sample.addressLine1", "address.addressLine1")
        .field("sample.addressLine2", "address.addressLine2")
        .field("sample.addressLine3", "address.addressLine3")
        .field("sample.townName", "address.townName")
        .field("sample.postcode", "address.postcode")
        .byDefault()
        .register();

    factory
        .classMap(CaseSampleSensitiveDTO.class, CaseUpdateSampleSensitive.class)
        .byDefault()
        .register();
    factory.classMap(CaseSampleDTO.class, CaseUpdateSample.class).byDefault().register();

    factory.classMap(NewCaseSampleSensitive.class, NewCaseDTO.class).byDefault().register();
    factory.classMap(AddressDTO.class, AddressCompact.class).byDefault().register();
    factory.classMap(Address.class, AddressCompact.class).byDefault().register();
    factory.classMap(NewCaseSample.class, NewCaseDTO.class).byDefault().register();
  }

  static class EstabTypeConverter extends BidirectionalConverter<String, EstabType> {
    @Override
    public String convertFrom(
        EstabType source, Type<String> destinationType, MappingContext mappingContext) {
      return source.name();
    }

    @Override
    public EstabType convertTo(
        String source, Type<EstabType> destinationType, MappingContext mappingContext) {
      return EstabType.forCode(source);
    }
  }
}
