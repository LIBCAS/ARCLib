package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.formatlibrary.domain.Format;

import java.util.List;

public interface FormatStore {
    Format findByFormatId(Integer formatId);

    Format create(Format entity);

    Format update(Format entity);

    Format find(String id);

    List<Format> findFormatsOfRisk(String riskId);

    void delete(Format format);
}
