package cz.inqool.uas.index;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.store.DomainStore;
import cz.inqool.uas.rest.data.DataAdapter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.inject.Inject;
import java.util.Collection;

@Getter
public class IndexedDomainStore<T extends DomainObject, Q extends EntityPathBase<T>, U extends IndexedDomainObject>
        extends DomainStore<T, Q> implements IndexedStore<T, U>, DataAdapter<T> {
    private ElasticsearchTemplate template;

    private Class<U> uType;

    public IndexedDomainStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }

    /**
     * Converts a JPA instance to an Elasticsearch instance.
     *
     * <p>
     *     Subclasses should call super to reuse the provided mapping for {@link DomainObject}
     * </p>
     * @param obj JPA instance
     * @return Elasticsearch instance
     */
    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getUType().newInstance();

        u.setId(obj.getId());

        return u;
    }

    @Inject
    public void setTemplate(ElasticsearchTemplate template) {
        this.template = template;
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
}
