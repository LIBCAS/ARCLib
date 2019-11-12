package cz.cas.lib.core.index.global;

import cz.cas.lib.core.index.solr.IndexedStore;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class GlobalReindexer {
    private List<IndexedStore> stores;

    private SolrTemplate template;

    /**
     * Reindexes all detected IndexedStores
     */
    @Transactional
    public void reindex() {
        reindexSubset(true, null);
    }

    /**
     * Reindexes only specified IndexedStores or all if not specified
     *
     * @param dropIndexes  specify if indexes should be dropped prior to reindexing
     * @param storeClasses store classes subset to reindex or null if all
     */
    @Transactional
    public void reindexSubset(boolean dropIndexes, List<Class<? extends IndexedStore>> storeClasses) {
        if (dropIndexes) {
            throw new UnsupportedOperationException();
        }

        stores.stream()
                .map(Utils::unwrap)
                .filter(store -> storeClasses == null || storeClasses.contains(store.getClass()))
                .forEach(store -> {
                    log.info("Reindexing store {}", store.getClass().getName());
                    store.reindex();
                    log.info("Reindexing complete");
                });
    }

    @Inject
    public void setStores(List<IndexedStore> stores) {
        this.stores = stores;
    }

    @Inject
    public void setTemplate(SolrTemplate template) {
        this.template = template;
    }
}
