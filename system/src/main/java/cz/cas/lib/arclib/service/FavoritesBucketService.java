package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.FavoritesBucket;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.service.tableexport.TableExportType;
import cz.cas.lib.arclib.service.tableexport.TableExporter;
import cz.cas.lib.arclib.store.FavoritesBucketStore;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoritesBucketService {
    private FavoritesBucketStore store;
    private SolrArclibXmlStore arclibXmlIndexStore;
    private TableExporter tableExporter;

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

    public void exportFavorites(String userId, Integer page, Integer pageSize, boolean ignorePagination, String name, List<String> columns, List<String> header, TableExportType format, HttpServletResponse response) {
        FavoritesBucket bucket = store.findFavoritesBucketOfUser(userId);
        List<IndexedArclibXmlDocument> data = new ArrayList<>();
        if (bucket != null) {
            Params params = new Params();
            params.setFilter(List.of(new Filter("id", FilterOperation.IN, String.join(",", bucket.getFavoriteIds()), List.of())));
            params.setPageSize(pageSize);
            params.setPage(page);
            Result<IndexedArclibXmlDocument> all = ignorePagination ? arclibXmlIndexStore.findAllIgnorePagination(params) : arclibXmlIndexStore.findAll(params);
            data = all.getItems();
        }
        List<List<Object>> table = data.stream().map(e -> e.getExportTableValues(columns)).collect(Collectors.toList());

        try (OutputStream out = response.getOutputStream()) {
            switch (format) {
                case XLSX:
                    tableExporter.exportXlsx(name, header, IndexedArclibXmlDocument.getExportTableConfig(columns), table, true, out);
                    break;
                case CSV:
                    tableExporter.exportCsv(name, header, table, out);
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Autowired
    public void setArclibXmlIndexStore(SolrArclibXmlStore arclibXmlIndexStore) {
        this.arclibXmlIndexStore = arclibXmlIndexStore;
    }

    @Autowired
    public void setStore(FavoritesBucketStore store) {
        this.store = store;
    }

    @Autowired
    public void setTableExporter(TableExporter tableExporter) {
        this.tableExporter = tableExporter;
    }
}
