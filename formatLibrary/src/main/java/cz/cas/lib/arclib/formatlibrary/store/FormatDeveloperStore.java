package cz.cas.lib.arclib.formatlibrary.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDeveloper;
import cz.cas.lib.arclib.formatlibrary.domain.QFormatDeveloper;
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
