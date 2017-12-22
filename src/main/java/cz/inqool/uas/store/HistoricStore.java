package cz.inqool.uas.store;

import com.google.common.collect.Ordering;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.dto.Order;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.store.revision.Revision;
import lombok.Getter;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asSet;

public abstract class HistoricStore<T extends DomainObject> {
    private static final String AUTHOR = "author";
    /**
     * Entity manager used for JPA
     */
    protected EntityManager entityManager;

    /**
     * Entity class object
     */
    @Getter
    protected Class<T> type;

    public HistoricStore(Class<T> type) {
        this.type = type;
    }

    public List<Revision> getRevisions(String id, Params params) {

        AuditReader reader = getAuditReader();

        List<Number> revisionNumbers = reader.getRevisions(type, id);
        Map<Number, Revision> revisionMap = reader.findRevisions(Revision.class, asSet(revisionNumbers));
        Collection<Revision> revisions = revisionMap.values();

        Comparator<Revision> timestampComparator = Comparator.comparingLong(Revision::getTimestamp);
        Comparator<Revision> authorComparator = Comparator.comparing(Revision::getAuthor);

        Comparator<Revision> comparator;
        if (AUTHOR.equals(params.getSort())) {
            comparator = authorComparator;
        } else {
            comparator = timestampComparator;
        }

        if (params.getOrder() == Order.DESC) {
            comparator = comparator.reversed();
        }

        return Ordering.from(comparator)
                       .sortedCopy(revisions);
    }

    public T findAtRevision(String id, long revisionId) {
        T instance = getAuditReader().find(type, id, revisionId);
        loadEntity(instance);
        return instance;
    }

    private AuditReader getAuditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    protected abstract void loadEntity(T entity);

    @Inject
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
