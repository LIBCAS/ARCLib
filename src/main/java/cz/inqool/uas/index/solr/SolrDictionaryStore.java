package cz.inqool.uas.index.solr;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.rest.data.DictionaryDataAdapter;
import cz.inqool.uas.store.DictionaryStore;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.data.solr.core.SolrTemplate;

import javax.inject.Inject;
import java.util.Collection;

import static cz.inqool.uas.util.Utils.toDate;

/**
 * Extension to {@link IndexedStore} providing {@link SolrDictionaryStore#toIndexObject(DictionaryObject)}
 * mapping for common attributes of {@link DictionaryObject}.
 *
 * @param <T> type of JPA entity
 * @param <Q> Type of query object
 * @param <U> type of corresponding Solr entity
 */
@Getter
public abstract class SolrDictionaryStore<T extends DictionaryObject, Q extends EntityPathBase<T>, U extends SolrDictionaryObject>
        extends DictionaryStore<T, Q> implements SolrStore<T, U>, DictionaryDataAdapter<T> {
    private SolrTemplate template;

    private Class<U> uType;

    public SolrDictionaryStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }


    /**
     * Converts a JPA instance to an Solr instance.
     *
     * <p>
     *     Subclasses should call super to reuse the provided mapping for {@link DictionaryObject}
     * </p>
     * @param obj JPA instance
     * @return Solr instance
     */
    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getUType().newInstance();

        u.setId(obj.getId());
        u.setCreated(toDate(obj.getCreated()));
        u.setUpdated(toDate(obj.getUpdated()));
        u.setName(obj.getName());
        u.setOrder(obj.getOrder());
        u.setActive(obj.getActive());

        return u;
    }

    @Override
    public T save(T entity) {
        entity = super.save(entity);
        return SolrStore.super.save(entity);
    }

    @Override
    public Collection<? extends T> save(Collection<? extends T> entities) {
        super.save(entities);
        return SolrStore.super.save(entities);
    }

    @Override
    public void delete(T entity) {
        super.delete(entity);
        SolrStore.super.delete(entity);
    }

    @Override
    public Result<T> findAll(Params params) {
        return SolrStore.super.findAll(params);
    }

    @Inject
    public void setTemplate(SolrTemplate template) {
        this.template = template;
    }
}