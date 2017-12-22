package cz.inqool.uas.index.global;

import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.store.Transactional;
import cz.inqool.uas.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class GlobalReindexer {
    private List<IndexedStore> stores;

    private ElasticsearchTemplate template;

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
     * @param dropIndexes specify if indexes should be dropped prior to reindexing
     * @param storeClasses store classes subset to reindex or null if all
     */
    @Transactional
    public void reindexSubset(boolean dropIndexes, List<Class<? extends IndexedStore>> storeClasses) {
        if (dropIndexes) {
            stores.stream()
                  .map(Utils::unwrap)
                  .filter(store -> storeClasses == null || storeClasses.contains(store.getClass()))
                  .map(IndexedStore::getIndexName)
                  .distinct()
                  .forEach(index -> {
                      if (template.indexExists(index)) {
                          log.info("Deleting index {}.", index);
                          template.deleteIndex(index);
                      }

                      log.info("Creating index {}.", index);
                      String settings = ElasticsearchTemplate.readFileFromClasspath("/es_settings.json");
                      template.createIndex(index, settings);
                  });
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
    public void setTemplate(ElasticsearchTemplate template) {
        this.template = template;
    }
}
