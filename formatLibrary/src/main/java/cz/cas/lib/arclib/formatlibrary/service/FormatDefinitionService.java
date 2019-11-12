package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.store.FormatDefinitionStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class FormatDefinitionService {
    private FormatDefinitionStore store;

    public FormatDefinition find(String id) {
        return store.find(id);
    }

    /**
     * Find formats by format id
     *
     * @param formatId        id of the format to search
     * @param localDefinition if <code>true</code> search in the local definitions, otherwise in the upstream definitions
     * @return list of the formats found
     */
    public List<FormatDefinition> findByFormatId(Integer formatId, boolean localDefinition) {
        return store.findByFormatId(formatId, localDefinition);
    }

    /**
     * Find the preferred definition of the format by format id
     *
     * @param formatId format id of the format to search
     * @return format definition with the given <code>formatId</code> and attribute <code>preferred</code> equal to <code>true</code>,
     * null if there is no such format found
     */
    public FormatDefinition findPreferredDefinitionByFormatId(Integer formatId) {
        return store.findPreferredByFormatId(formatId);
    }

    /**
     * Find the preferred definition of the format by PUID
     *
     * @param puid PUID of the format to search
     * @return format with the given <code>PUID</code> and attribute <code>preferred</code> equal to <code>true</code>,
     * null if there is no such format found
     */
    public FormatDefinition findPreferredDefinitionsByPuid(String puid) {
        return store.findPreferredByFormatPuid(puid);
    }

    public Collection<FormatDefinition> findAll() {
        return store.findAll();
    }

    public FormatDefinition create(FormatDefinition d) {
        return store.create(d);
    }

    public FormatDefinition update(FormatDefinition d) {
        return store.update(d);
    }


    /**
     * Saves format definition and performs the versioning
     *
     * @param formatDefinition format definition to save
     * @return pair of [format definition, message describing the versioning of the format definition]
     */
    @Transactional
    public Pair<FormatDefinition, String> saveWithVersioning(FormatDefinition formatDefinition) {
        Integer formatId = formatDefinition.getFormat().getFormatId();
        Collection<FormatDefinition> localDefinitions = findByFormatId(formatId, true);
        Collection<FormatDefinition> upstreamDefinitions = findByFormatId(formatId, false);
        String message;
        boolean newDefCreated = true;

        /**
         * Case 1: both upstream definitions and local definitions are empty
         */
        if (localDefinitions.isEmpty() && upstreamDefinitions.isEmpty()) {
            formatDefinition.setPreferred(true);
            formatDefinition.setInternalVersionNumber(1);
            formatDefinition.setPreviousInternalDefinition(null);
            message = "new definition created";
        } else {
            if (formatDefinition.isLocalDefinition()) {
                if (!localDefinitions.isEmpty()) {
                    FormatDefinition highestVersionLocalDefinition = localDefinitions.stream()
                            .max(Comparator.comparing(FormatDefinition::getInternalVersionNumber))
                            .get();
                    if (highestVersionLocalDefinition.equals(formatDefinition)) {
                        newDefCreated = false;
                        boolean providedDefinitionPreferred = formatDefinition.isPreferred();
                        formatDefinition = highestVersionLocalDefinition;
                        message = "recent local definition is equal to the current definition";
                        if (providedDefinitionPreferred != formatDefinition.isPreferred()) {
                            formatDefinition.setPreferred(providedDefinitionPreferred);
                            message = message + ", preferred flag has changed to: " + providedDefinitionPreferred;
                        }
                    } else {
                        formatDefinition.setInternalVersionNumber(highestVersionLocalDefinition.getInternalVersionNumber() + 1);
                        formatDefinition.setPreviousInternalDefinition(highestVersionLocalDefinition);
                        message = "has been updated with a local definition";
                    }
                } else {
                    formatDefinition.setInternalVersionNumber(1);
                    formatDefinition.setPreviousInternalDefinition(null);
                    message = "has been updated with the local definition";
                }
            } else {
                if (!upstreamDefinitions.isEmpty()) {
                    FormatDefinition highestVersionUpstreamDefinition = upstreamDefinitions.stream()
                            .max(Comparator.comparing(FormatDefinition::getInternalVersionNumber))
                            .get();
                    if (highestVersionUpstreamDefinition.equals(formatDefinition)) {
                        newDefCreated = false;
                        formatDefinition = highestVersionUpstreamDefinition;
                        message = "recent upstream definition is equal to the current definition";
                    } else {
                        formatDefinition.setPreferred(highestVersionUpstreamDefinition.isPreferred());
                        formatDefinition.setInternalVersionNumber(highestVersionUpstreamDefinition.getInternalVersionNumber() + 1);
                        formatDefinition.setPreviousInternalDefinition(highestVersionUpstreamDefinition);
                        message = "has been updated with the recent upstream definition";
                    }
                } else {
                    formatDefinition.setPreferred(true);
                    formatDefinition.setInternalVersionNumber(1);
                    formatDefinition.setPreviousInternalDefinition(null);
                    message = "has been updated with the recent upstream definition";
                }
            }
            /**
             * If preferred, set the previously preferred as non preferred
             */
            if (formatDefinition.isPreferred()) {
                FormatDefinition preferredDefinition = findPreferredDefinitionByFormatId(formatId);
                if (preferredDefinition != null && !preferredDefinition.getId().equals(formatDefinition.getId())) {
                    preferredDefinition.setPreferred(false);
                    store.update(preferredDefinition);
                }
            }
        }
        if (newDefCreated)
            formatDefinition = store.create(formatDefinition);
        else
            formatDefinition = store.update(formatDefinition);
        log.debug("Format with format id " + formatId + ": " + message);
        return Pair.of(formatDefinition, message);
    }

    @Inject
    public void setStore(FormatDefinitionStore store) {
        this.store = store;
    }
}
