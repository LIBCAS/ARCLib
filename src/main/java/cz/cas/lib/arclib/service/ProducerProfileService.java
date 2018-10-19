package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ProducerProfileService implements DelegateAdapter<ProducerProfile> {
    @Getter
    private ProducerProfileStore delegate;

    @Transactional
    public ProducerProfile findByExternalId(String number) {
        return delegate.findByExternalId(number);
    }

        @Inject
    public void setDelegate(ProducerProfileStore delegate) {
        this.delegate = delegate;
    }
}
