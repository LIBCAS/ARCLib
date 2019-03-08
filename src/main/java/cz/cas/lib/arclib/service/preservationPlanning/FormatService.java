package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.preservationPlanning.Format;
import cz.cas.lib.arclib.store.FormatStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class FormatService implements DelegateAdapter<Format> {
    @Getter
    private FormatStore delegate;

    /**
     * Find formats by format id
     *
     * @param formatId id of the format to search
     * @return list of the formats found
     */
    public Format findByFormatId(Integer formatId) {
        return delegate.findByFormatId(formatId);
    }

    @Override
    @Transactional
    public Format save(Format entity) {
        return delegate.save(entity);
    }

    @Inject
    public void setFormatStore(FormatStore formatStore) {
        this.delegate = formatStore;
    }
}
