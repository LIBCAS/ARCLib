package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.preservationPlanning.QRelatedFormat;
import cz.cas.lib.arclib.domain.preservationPlanning.RelatedFormat;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RelatedFormatStore
        extends DatedStore<RelatedFormat, QRelatedFormat> {
    public RelatedFormatStore() {
        super(RelatedFormat.class, QRelatedFormat.class);
    }

    public List<RelatedFormat> findByRelatedFormatId(Integer relatedFormatId) {
        QRelatedFormat relatedFormat = qObject();

        JPAQuery<RelatedFormat> query = query()
                .select(relatedFormat)
                .where(relatedFormat.relatedFormatId.eq(relatedFormatId));

        List<RelatedFormat> all = query.fetch();

        detachAll();
        return all;
    }
}
