package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;

import java.util.Collection;
import java.util.List;

public interface FormatDefinitionStore {
    List<FormatDefinition> findByFormatId(Integer formatId, Boolean localDefinition);

    FormatDefinition findPreferredByFormatPuid(String puid);

    FormatDefinition findPreferredByFormatId(Integer formatId);

    Collection<FormatDefinition> findAll();

    FormatDefinition find(String id);

    FormatDefinition create(FormatDefinition entity);

    FormatDefinition update(FormatDefinition entity);

    void delete(FormatDefinition formatDefinition);
}
