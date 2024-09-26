package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.formatlibrary.domain.FormatDeveloper;
import cz.cas.lib.arclib.formatlibrary.store.FormatDeveloperStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.Optional;

@Service
public class FormatDeveloperService {
    private FormatDeveloperStore store;

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

        return store.save(formatDeveloper);
    }

    @Transactional
    public FormatDeveloper findHighestInternalVersionByDeveloperId(Integer developerId) {
        Optional<FormatDeveloper> developer = store.findByDeveloperId(developerId).stream()
                .max(Comparator.comparing(FormatDeveloper::getInternalVersionNumber));
        return developer.orElse(null);
    }

    @Transactional
    public FormatDeveloper save(FormatDeveloper entity) {
        return store.save(entity);
    }

    @Autowired
    public void setStore(FormatDeveloperStore store) {
        this.store = store;
    }
}
