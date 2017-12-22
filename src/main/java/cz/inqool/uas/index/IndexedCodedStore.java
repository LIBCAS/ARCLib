package cz.inqool.uas.index;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.uas.domain.CodedObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.rest.data.DictionaryDataAdapter;
import cz.inqool.uas.store.CodedStore;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.inject.Inject;
import java.util.Collection;

import static cz.inqool.uas.util.Utils.toLabeledReference;

/**
 * Extension to {@link IndexedStore} providing {@link IndexedCodedStore#toIndexObject(CodedObject)}
 * mapping for common attributes of {@link CodedObject}.
 *
 * @param <T> type of JPA entity
 * @param <Q> Type of query object
 * @param <U> type of corresponding Elasticsearch entity
 */
@Getter
public abstract class IndexedCodedStore<T extends CodedObject<V>, Q extends EntityPathBase<T>, U extends IndexedCodedObject, V extends Enum<V> & Labeled>
        extends CodedStore<T, Q, V> implements IndexedStore<T, U>, DictionaryDataAdapter<T> {
    private ElasticsearchTemplate template;

    private Class<U> uType;

    public IndexedCodedStore(Class<T> type, Class<Q> qType, Class<V> cType, Class<U> uType) {
        super(type, qType, cType);
        this.uType = uType;
    }


    /**
     * Converts a JPA instance to an Elasticsearch instance.
     *
     * <p>
     *     Subclasses should call super to reuse the provided mapping for {@link CodedObject}
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
        u.setCode(toLabeledReference(obj.getCode()));

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