package cz.cas.lib.arclib.formatlibrary.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.formatlibrary.domain.FormatRelationshipType;
import cz.cas.lib.arclib.formatlibrary.domain.QRelatedFormat;
import cz.cas.lib.arclib.formatlibrary.domain.RelatedFormat;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class RelatedFormatStore
        extends DatedStore<RelatedFormat, QRelatedFormat> {
    public RelatedFormatStore() {
        super(RelatedFormat.class, QRelatedFormat.class);
    }

    public RelatedFormat findByRelatedFormatIdAndRelationType(Integer relatedFormatId, FormatRelationshipType relationshipType) {
        QRelatedFormat relatedFormat = qObject();

        JPAQuery<RelatedFormat> query = query()
                .select(relatedFormat)
                .where(relatedFormat.relationshipType.eq(relationshipType))
                .where(relatedFormat.relatedFormatId.eq(relatedFormatId));

        RelatedFormat format = query.fetchFirst();

        detachAll();
        return format;
    }
}
