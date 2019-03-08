package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatDefinition;
import cz.cas.lib.arclib.store.FormatDefinitionStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class FormatDefinitionService implements DelegateAdapter<FormatDefinition> {
    @Getter
    private FormatDefinitionStore delegate;

    /**
     * Find formats by format id
     *
     * @param formatId        id of the format to search
     * @param localDefinition if <code>true</code> search in the local definitions, otherwise in the upstream definitions
     * @return list of the formats found
     */
    public List<FormatDefinition> findByFormatId(Integer formatId, boolean localDefinition) {
        return delegate.findByFormatId(formatId, localDefinition);
    }

    /**
     * Find the preferred definition of the format by format id
     *
     * @param formatId format id of the format to search
     * @return format definition with the given <code>formatId</code> and attribute <code>preferred</code> equal to <code>true</code>,
     * null if there is no such format found
     */
    public FormatDefinition findPreferredDefinitionByFormatId(Integer formatId) {
        return delegate.findPreferredByFormatId(formatId);
    }

    /**
     * Find the preferred definition of the format by PUID
     *
     * @param puid PUID of the format to search
     * @return format with the given <code>PUID</code> and attribute <code>preferred</code> equal to <code>true</code>,
     * null if there is no such format found
     */
    public FormatDefinition findPreferredDefinitionsByPuid(String puid) {
        return delegate.findPreferredByFormatPuid(puid);
    }

    /**
     * Saves format definition to database.
     * <p>
     * If attribute 'preferred' is <code>true</code> the currently preferred version is set to not preferred (if exists),
     * if there is no currently preferred version, this version is set to preferred
     *
     * @param formatDefinition entity to save
     * @return saved entity
     */
    @Override
    @Transactional
    public FormatDefinition save(FormatDefinition formatDefinition) {
        Integer formatId = formatDefinition.getFormat().getFormatId();
        Integer internalVersionNumber = formatDefinition.getInternalVersionNumber();
        boolean localDefinition = formatDefinition.isLocalDefinition();

        FormatDefinition previouslyPreferred = findPreferredDefinitionByFormatId(formatId);
        if (previouslyPreferred == null) {
            formatDefinition.setPreferred(true);
            log.debug("Format definition of format with format id " + formatId + ", internal version number " +
                    internalVersionNumber + ", local definition '" + localDefinition + "': has been set as the preferred format definition.");
        }
        if (previouslyPreferred != null && formatDefinition.isPreferred()) {
            previouslyPreferred.setPreferred(false);
            save(previouslyPreferred);
            log.debug("Format definition of format with format id " + formatId + ", internal version number " +
                    internalVersionNumber + ", local definition '" + localDefinition + "': has been set as the preferred format definition.");
        }
        return delegate.save(formatDefinition);
    }

    @Inject
    public void setDelegate(FormatDefinitionStore delegate) {
        this.delegate = delegate;
    }
}
