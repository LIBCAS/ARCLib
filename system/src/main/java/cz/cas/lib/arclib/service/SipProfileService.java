package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.dto.SipProfileDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.store.Transactional;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class SipProfileService {
    private SipProfileStore store;
    private BeanMappingService beanMappingService;
    private UserDetails userDetails;

    /**
     * Validates field {@link SipProfile#xsl} against XSD and in case of success saves sip profile
     *
     * @param sipProfile sip profile to save
     * @return saved sip profile
     */
    @Transactional
    public SipProfile save(SipProfile sipProfile) throws DocumentException {
        notNull(sipProfile.getProducer(), () -> new BadRequestException("SipProfile must have producer assigned"));
        // if user is not SUPER_ADMIN then entity must be assigned to user's producer
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            eq(sipProfile.getProducer().getId(), userDetails.getUser().getProducer().getId(), () -> new ForbiddenOperation("Producer of SipProfile must be the same as producer of logged-in user."));
        }

        SAXReader reader = new SAXReader();
        reader.read(new ByteArrayInputStream(sipProfile.getXsl().getBytes(StandardCharsets.UTF_8)));

        SipProfile sipProfileFound = store.find(sipProfile.getId());
        if (sipProfileFound != null) {
            // if user is not SUPER_ADMIN then change of producer is forbidden
            if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
                eq(sipProfile.getProducer().getId(), sipProfileFound.getProducer().getId(), () -> new ForbiddenOperation("Cannot change SipProfile's Producer"));
            }
            if (!sipProfileFound.isEditable()) {
                throw new ForbiddenOperation(ValidationProfile.class, sipProfile.getId());
            }
        }
        if (sipProfileFound != null && !sipProfileFound.isEditable()) {
            throw new ForbiddenOperation(SipProfile.class, sipProfile.getId());
        }

        sipProfile.setEditable(true);
        return store.save(sipProfile);
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

    public SipProfile findWithDeletedFilteringOff(String id) {
        return store.findWithDeletedFilteringOff(id);
    }

    public SipProfile findByExternalId(String externalId) {
        return store.findByExternalId(externalId);
    }

    public Collection<SipProfile> findAll() {
        return store.findAll();
    }


    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setStore(SipProfileStore store) {
        this.store = store;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

}
