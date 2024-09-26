package cz.cas.lib.core.index.solr;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
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

import static cz.cas.lib.core.util.Utils.toDate;

@Getter
public abstract class IndexedNamedStore<T extends NamedObject, Q extends EntityPathBase<T>, U extends IndexedNamedObject>
        extends NamedStore<T, Q> implements IndexedStore<T, U>, DataAdapter<T> {
    private SolrClient solrClient;

    private Class<U> uType;

    public IndexedNamedStore(Class<T> type, Class<Q> qType, Class<U> uType) {
        super(type, qType);
        this.uType = uType;
    }

    @SneakyThrows
    public U toIndexObject(T obj) {
        U u = getIndexObjectInstance();
        u.setId(obj.getId());
        u.setType(getIndexType());
        u.setCreated(toDate(obj.getCreated()));
        u.setUpdated(toDate(obj.getUpdated()));
        u.setName(obj.getName());
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


    @Autowired
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    @Override
    @PostConstruct
    public void init() {
        analyzeIndexedClass();
    }
}