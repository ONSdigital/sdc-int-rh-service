package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import ma.glasnost.orika.BoundMapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {

  private static final Logger log = LoggerFactory.getLogger(UniqueAccessCodeService.class);

  @Autowired private RespondentDataService dataRepo;
  private BoundMapperFacade<UAC, UniqueAccessCodeDTO> uacMapperFacade;
  private BoundMapperFacade<CollectionCase, UniqueAccessCodeDTO> caseMapperFacade;

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {

    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
    ConverterFactory converterFactory = mapperFactory.getConverterFactory();
    converterFactory.registerConverter(new StringToUUIDConverter());
    this.uacMapperFacade = mapperFactory.getMapperFacade(UAC.class, UniqueAccessCodeDTO.class);
    this.caseMapperFacade =
        mapperFactory.getMapperFacade(CollectionCase.class, UniqueAccessCodeDTO.class);
  }

  @Override
  public UniqueAccessCodeDTO getUniqueAccessCodeData(String uac) throws CTPException {

    UniqueAccessCodeDTO data = new UniqueAccessCodeDTO();
    data.setUac(uac);
    Optional<UAC> uacMatch = dataRepo.readUAC(getSha256Hash(uac));
    if (uacMatch.isPresent()) {
      uacMapperFacade.map(uacMatch.get(), data);
      if (!StringUtils.isEmpty(data.getCaseId())) {
        Optional<CollectionCase> caseMatch =
            dataRepo.readCollectionCase(uacMatch.get().getCaseId());
        if (caseMatch.isPresent()) {
          caseMapperFacade.map(caseMatch.get(), data);
        } else {
          log.warn("Failed to retrieve Case for UAC from storage");
        }
      } else {
        log.warn("Retrieved UAC CaseId not present");
      }
    } else {
      log.warn("Failed to retrieve UAC from storage.");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }

    return data;
  }

  /**
   * Create the SHA256 Hash of the UAC
   *
   * @param uac
   * @return SHA256 Hash
   * @throws CTPException
   */
  private String getSha256Hash(String uac) throws CTPException {

    String uacHash = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hashInBytes = md.digest(uac.getBytes(StandardCharsets.UTF_8));

      // bytes to hex
      StringBuilder sb = new StringBuilder();
      for (byte b : hashInBytes) {
        sb.append(String.format("%02x", b));
      }
      uacHash = sb.toString();
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA256 Hashing error for UAC");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ex.getMessage());
    }

    if (uacHash.length() != 64) {
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND);
    }

    return uacHash;
  }
}