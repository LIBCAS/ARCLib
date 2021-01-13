package cz.cas.lib.core.index.solr;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteAware;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DataAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.data.solr.core.SolrTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.toDate;

@Getter
@Transactional
public abstract class IndexedDatedStore<T extends DatedObject, Q extends EntityPathBase<T>, U extends IndexedDatedObject>
        extends DatedStore<T, Q> implements IndexedStore<T, U>, DataAdapter<T> {
    protected SolrTemplate template;

    private Class<U> uType;

    public IndexedDatedStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }

    /**
     * Converts a JPA instance to an Solr instance.
     * <p>
     * <p>
     * Subclasses should call super to reuse the provided mapping for {@link DatedObject}
     * </p>
     *
     * @param obj JPA instance
     * @return Solr instance
     */
    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getIndexObjectInstance();
        u.setType(getIndexType());
        u.setId(obj.getId());
        u.setCreated(toDate(obj.getCreated()));
        u.setUpdated(toDate(obj.getUpdated()));
        if (obj instanceof AutoCompleteAware) {
            u.setAutoCompleteLabel(((AutoCompleteAware) obj).getAutoCompleteLabel());
        }
        return u;
    }

    @Override
    public T save(T entity) {
        entity = super.save(entity);
        return IndexedStore.super.save(entity);
    }

    @Override
    public Collection<? extends T> save(Collection<? extends T> entities) {
        super.save(entities);
        return IndexedStore.super.save(entities);
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
    public void setTemplate(SolrTemplate template) {
        this.template = template;
    }

    @Override
    @PostConstruct
    public void init() {
        analyzeIndexedClass();
    }
}
