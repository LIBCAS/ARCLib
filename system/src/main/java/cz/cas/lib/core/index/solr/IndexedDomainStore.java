package cz.cas.lib.core.index.solr;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteAware;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DataAdapter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.solr.client.solrj.SolrClient;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

@Getter
public abstract class IndexedDomainStore<T extends DomainObject, Q extends EntityPathBase<T>, U extends IndexedDomainObject>
        extends DomainStore<T, Q> implements IndexedStore<T, U>, DataAdapter<T> {
    private SolrClient solrClient;

    private Class<U> uType;

    public IndexedDomainStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }

    /**
     * Converts a JPA instance to an Solr instance.
     *
     * <p>
     * Subclasses should call super to reuse the provided mapping for {@link DomainObject}
     * </p>
     *
     * @param obj JPA instance
     * @return Solr instance
     */
    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getIndexObjectInstance();
        u.setId(obj.getId());
        u.setType(getIndexType());
        if (obj instanceof AutoCompleteAware) {
            u.setAutoCompleteLabel(((AutoCompleteAware) obj).getAutoCompleteLabel());
        }
        return u;
    }

    @Autowired
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
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
    public void hardDelete(T entity) {
        delete(entity);
    }


    @Override
    public Result<T> findAll(Params params) {
        return IndexedStore.super.findAll(params);
    }

    @Override
    @PostConstruct
    public void init() {
        analyzeIndexedClass();
    }
}
