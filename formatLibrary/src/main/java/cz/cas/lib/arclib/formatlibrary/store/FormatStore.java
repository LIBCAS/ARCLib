package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.formatlibrary.domain.Format;

import java.util.Collection;
import java.util.List;

public interface FormatStore {
    Format findByFormatId(Integer formatId);

    Format create(Format entity);

    Format update(Format entity);

    Format find(String id);

    List<Format> findAllInList(List<String> ids);

    List<Format> findFormatsOfRisk(String riskId);

    Collection<Format> findAll();

    void delete(Format format);
}
