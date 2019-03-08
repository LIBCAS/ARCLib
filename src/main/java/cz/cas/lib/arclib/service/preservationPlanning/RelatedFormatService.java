package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.preservationPlanning.RelatedFormat;
import cz.cas.lib.arclib.store.RelatedFormatStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Optional;

@Service
public class RelatedFormatService implements DelegateAdapter<RelatedFormat> {
    @Getter
    private RelatedFormatStore delegate;

    /**
     * Creates new version of related format if the related format from external does not equal
     * the highest version of related format in DB
     *
     * @param external related format from external
     * @return the new version of related format or the existing version of related format respectively
     */
    @Transactional
    public RelatedFormat updateFromExternal(RelatedFormat external) {
        RelatedFormat relatedFormatFound = findHighestVersionByRelatedFormatId(external.getRelatedFormatId());
        if (external.equals(relatedFormatFound)) return relatedFormatFound;

        RelatedFormat relatedFormat = new RelatedFormat();

        Integer internalVersionNumber = relatedFormatFound != null ?
                relatedFormatFound.getInternalVersionNumber() + 1 : 1;
        relatedFormat.setInternalVersionNumber(internalVersionNumber);
        relatedFormat.setRelatedFormatId(external.getRelatedFormatId());
        relatedFormat.setRelatedFormatVersion(external.getRelatedFormatVersion());
        relatedFormat.setRelatedFormatName(external.getRelatedFormatName());
        relatedFormat.setRelationshipType(external.getRelationshipType());

        return delegate.save(relatedFormat);
    }

    @Transactional
    public RelatedFormat findHighestVersionByRelatedFormatId(Integer relatedFormatId) {
        Optional<RelatedFormat> relatedFormat = delegate.findByRelatedFormatId(relatedFormatId).stream()
                .max(Comparator.comparing(RelatedFormat::getInternalVersionNumber));
        return relatedFormat.orElse(null);
    }

    @Override
    @Transactional
    public RelatedFormat save(RelatedFormat entity) {
        return delegate.save(entity);
    }

    @Inject
    public void setDelegate(RelatedFormatStore delegate) {
        this.delegate = delegate;
    }
}
