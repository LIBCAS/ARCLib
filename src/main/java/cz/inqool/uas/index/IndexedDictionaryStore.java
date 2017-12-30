package cz.inqool.uas.index;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.rest.data.DictionaryDataAdapter;
import cz.inqool.uas.store.DictionaryStore;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Extension to {@link IndexedStore} providing {@link IndexedDictionaryStore#toIndexObject(DictionaryObject)}
 * mapping for common attributes of {@link DictionaryObject}.
 *
 * @param <T> type of JPA entity
 * @param <Q> Type of query object
 * @param <U> type of corresponding Elasticsearch entity
 */
@Getter
public abstract class IndexedDictionaryStore<T extends DictionaryObject, Q extends EntityPathBase<T>, U extends IndexedDictionaryObject>
        extends DictionaryStore<T, Q> implements IndexedStore<T, U>, DictionaryDataAdapter<T> {
    private ElasticsearchTemplate template;

    private Class<U> uType;

    public IndexedDictionaryStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }


    /**
     * Converts a JPA instance to an Elasticsearch instance.
     *
     * <p>
     *     Subclasses should call super to reuse the provided mapping for {@link DictionaryObject}
     * </p>
     * @param obj JPA instance
     * @return Elasticsearch instance
     */
    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getUType().newInstance();

        u.setId(obj.getId());
        u.setCreated(obj.getCreated());
        u.setUpdated(obj.getUpdated());
        u.setName(obj.getName());
        u.setOrder(obj.getOrder());
        u.setActive(obj.getActive());

        return u;
    }

    @Override
    public T save(T entity) {
        entity = super.save(entity);
        return IndexedStore.super.save(entity);
    }

    @Override
    public Collection<? extends T> save(Collection<? extends T> entities) {
        Collection<? extends T> saved = super.save(entities);
        return IndexedStore.super.save(saved);
    }

    @Override
    public void delete(T entity) {
        super.delete(entity);
        IndexedStore.super.delete(entity);
    }

    @Override
    public Result<T> findAll(Params params) {
        return IndexedStore.super.findAll(params);
    }

    @Inject
    public void setTemplate(ElasticsearchTemplate template) {
        this.template = template;
    }
}