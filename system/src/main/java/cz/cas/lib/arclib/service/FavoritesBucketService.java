package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.FavoritesBucket;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.store.FavoritesBucketStore;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@Service
public class FavoritesBucketService {
    private FavoritesBucketStore store;
    private SolrArclibXmlStore arclibXmlIndexStore;

    public FavoritesBucket find(String id) {
        return store.find(id);
    }

    public Set<String> findFavoriteIdsOfUser(String userId) {
        FavoritesBucket bucket = store.findFavoritesBucketOfUser(userId);
        return bucket == null ? Set.of() : bucket.getFavoriteIds();
    }

    @Transactional
    public void saveFavoriteIdsOfUser(String userId, Set<String> ids) {
        FavoritesBucket bucket = store.findFavoritesBucketOfUser(userId);
        if (bucket == null) {
            bucket = new FavoritesBucket(new User(userId), ids);
        } else {
            bucket.setFavoriteIds(ids);
        }
        store.save(bucket);
    }

    public Result<IndexedArclibXmlDocument> getFavorites(String userId, Integer page, Integer pageSize) {
        FavoritesBucket bucket = store.findFavoritesBucketOfUser(userId);
        if (bucket == null) {
            return new Result<>(List.of(), 0L);
        } else {
            Params params = new Params();
            params.setFilter(List.of(new Filter("id", FilterOperation.IN, String.join(",", bucket.getFavoriteIds()), List.of())));
            params.setPageSize(pageSize);
            params.setPage(page);
            return arclibXmlIndexStore.findAll(params);
        }
    }

    @Inject
    public void setArclibXmlIndexStore(SolrArclibXmlStore arclibXmlIndexStore) {
        this.arclibXmlIndexStore = arclibXmlIndexStore;
    }

    @Inject
    public void setStore(FavoritesBucketStore store) {
        this.store = store;
    }
}
