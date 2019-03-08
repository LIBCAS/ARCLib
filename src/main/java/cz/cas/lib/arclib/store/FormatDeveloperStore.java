package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatDeveloper;
import cz.cas.lib.arclib.domain.preservationPlanning.QFormatDeveloper;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormatDeveloperStore
        extends DatedStore<FormatDeveloper, QFormatDeveloper> {
    public FormatDeveloperStore() {
        super(FormatDeveloper.class, QFormatDeveloper.class);
    }

    public List<FormatDeveloper> findByDeveloperId(Integer developerId) {
        QFormatDeveloper formatDeveloper = qObject();

        JPAQuery<FormatDeveloper> query = query()
                .select(formatDeveloper)
                .where(formatDeveloper.developerId.eq(developerId));

        List<FormatDeveloper> all = query.fetch();

        detachAll();

        return all;
    }
}
