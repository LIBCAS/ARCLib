package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.dto.SipProfileDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.store.Transactional;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class SipProfileService {
    private SipProfileStore store;
    private BeanMappingService beanMappingService;
    private UserDetails userDetails;

    @Transactional
    public SipProfile save(SipProfile sipProfile) {
        SipProfile sipProfileFound = store.find(sipProfile.getId());
        if (sipProfileFound != null && !sipProfileFound.isEditable()) {
            throw new ForbiddenOperation(SipProfile.class, sipProfile.getId());
        }
        return store.save(sipProfile);
    }

    /**
     * Validates field {@link SipProfile#xsl} against XSD and in case of success saves sip profile
     *
     * @param sipProfile sip profile to save
     * @return saved sip profile
     */
    @Transactional
    public SipProfile validateAndSave(SipProfile sipProfile) throws DocumentException {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            sipProfile.setProducer(new Producer(userDetails.getProducerId()));
        } else {
            notNull(sipProfile.getProducer(), () -> new BadRequestException("SipProfile has to have producer assigned"));
        }

        SAXReader reader = new SAXReader();
        reader.read(new ByteArrayInputStream(sipProfile.getXsl().getBytes(StandardCharsets.UTF_8)));
        return save(sipProfile);
    }


    public Collection<SipProfileDto> listSipProfileDtos() {
        Collection<SipProfile> all = this.findFilteredByProducer();
        return beanMappingService.mapTo(all, SipProfileDto.class);
    }

    public Collection<SipProfile> findFilteredByProducer() {
        if (hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }

    @Transactional
    public void delete(SipProfile entity) {
        store.delete(entity);
    }

    public SipProfile find(String id) {
        return store.find(id);
    }

    public SipProfile findByExternalId(String externalId) {
        return store.findByExternalId(externalId);
    }

    public Collection<SipProfile> findAll() {
        return store.findAll();
    }


    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

}
