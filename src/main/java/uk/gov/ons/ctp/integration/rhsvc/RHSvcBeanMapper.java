package uk.gov.ons.ctp.integration.rhsvc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
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
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.NewCaseSample;
import uk.gov.ons.ctp.common.event.model.NewCaseSampleSensitive;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;
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
    converterFactory.registerConverter(new LocalDateTimeConverter());
    converterFactory.registerConverter(new EstabTypeConverter());

    factory
        .classMap(UacUpdate.class, UniqueAccessCodeDTO.class)
        .field("metadata.wave", "wave")
        .byDefault()
        .register();

    factory
        .classMap(CollectionExercise.class, CollectionExerciseDTO.class)
        .field("metadata.numberOfWaves", "numberOfWaves")
        .field("metadata.waveLength", "waveLength")
        .field("metadata.cohorts", "cohorts")
        .field("metadata.cohortSchedule", "cohortSchedule")
        .byDefault()
        .register();

    factory.classMap(NewCaseSampleSensitive.class, NewCaseDTO.class).byDefault().register();
    factory.classMap(AddressDTO.class, AddressCompact.class).byDefault().register();
    factory.classMap(Address.class, AddressCompact.class).byDefault().register();
    factory.classMap(NewCaseSample.class, NewCaseDTO.class).byDefault().register();
    factory.classMap(SurveyUpdate.class, SurveyLiteDTO.class).byDefault().register();
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

  static class LocalDateTimeConverter extends BidirectionalConverter<Date, LocalDateTime> {

    @Override
    public LocalDateTime convertTo(
        Date date, Type<LocalDateTime> type, MappingContext mappingContext) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of(ZoneOffset.UTC.getId()));
    }

    @Override
    public Date convertFrom(
        LocalDateTime localDateTime, Type<Date> type, MappingContext mappingContext) {
      return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }
  }
}
