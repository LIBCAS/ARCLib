package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatDeveloper;
import cz.cas.lib.arclib.store.FormatDeveloperStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Optional;

@Service
public class FormatDeveloperService implements DelegateAdapter<FormatDeveloper> {
    @Getter
    private FormatDeveloperStore delegate;

    /**
     * Creates new version of format developer if the related format from external does not equal
     * the highest version of format developer in DB
     *
     * @param external format developer from external
     * @return the new version of format developer or the existing version of format developer respectively
     */
    @Transactional
    public FormatDeveloper updateFromExternal(FormatDeveloper external) {
        FormatDeveloper formatDeveloperFound = findHighestInternalVersionByDeveloperId(external.getDeveloperId());
        if (external.equals(formatDeveloperFound)) return formatDeveloperFound;

        FormatDeveloper formatDeveloper = new FormatDeveloper();

        Integer internalVersionNumber = formatDeveloperFound != null ?
                formatDeveloperFound.getInternalVersionNumber() + 1 : 1;
        formatDeveloper.setInternalVersionNumber(internalVersionNumber);
        formatDeveloper.setDeveloperId(external.getDeveloperId());
        formatDeveloper.setDeveloperCompoundName(external.getDeveloperCompoundName());
        formatDeveloper.setOrganisationName(external.getOrganisationName());
        formatDeveloper.setDeveloperName(external.getDeveloperName());

        return delegate.save(formatDeveloper);
    }

    @Transactional
    public FormatDeveloper findHighestInternalVersionByDeveloperId(Integer developerId) {
        Optional<FormatDeveloper> developer = delegate.findByDeveloperId(developerId).stream()
                .max(Comparator.comparing(FormatDeveloper::getInternalVersionNumber));
        return developer.orElse(null);
    }

    @Override
    @Transactional
    public FormatDeveloper save(FormatDeveloper entity) {
        return delegate.save(entity);
    }

    @Inject
    public void setDelegate(FormatDeveloperStore delegate) {
        this.delegate = delegate;
    }
}
