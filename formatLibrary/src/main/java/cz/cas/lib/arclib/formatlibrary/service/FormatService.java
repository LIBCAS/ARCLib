package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.store.FormatStore;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FormatService {
    @Getter
    private FormatStore store;

    /**
     * Find formats by format id
     *
     * @param formatId id of the format to search
     * @return list of the formats found
     */
    public Format findByFormatId(Integer formatId) {
        return store.findByFormatId(formatId);
    }

    @Transactional
    public Format create(Format entity) {
        return store.create(entity);
    }

    @Transactional
    public Format update(Format entity) {
        return store.update(entity);
    }

    @Inject
    public void setFormatStore(FormatStore formatStore) {
        this.store = formatStore;
    }

    public List<Format> findFormatsOfRisk(String riskId){
        return store.findFormatsOfRisk(riskId);
    }
}
