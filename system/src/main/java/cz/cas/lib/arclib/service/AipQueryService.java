package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.export.ExportConfig;
import cz.cas.lib.arclib.domain.export.ExportRoutine;
import cz.cas.lib.arclib.domain.export.ExportScope;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipQueryDetailDto;
import cz.cas.lib.arclib.dto.AipQueryDetailExportRoutineDto;
import cz.cas.lib.arclib.dto.AipQueryDto;
import cz.cas.lib.arclib.dto.BucketExportRequestDto;
import cz.cas.lib.arclib.index.IndexedArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ExportRoutineStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class AipQueryService {

    private static final String BUCKET_QUERY_API_ID = "bucket";

    private AipQueryStore store;
    private FavoritesBucketService favoritesBucketService;
    private ExportRoutineStore exportRoutineStore;
    private IndexedArclibXmlStore indexArclibXmlStore;
    private BeanMappingService beanMappingService;
    private AipExportService aipExportService;

    private UserDetails userDetails;


    public AipQueryDetailDto find(String id) {
        AipQuery aipQuery = store.find(id);
        if (aipQuery == null) {
            return null;
        }
        ExportRoutine routine = exportRoutineStore.findByAipQueryId(aipQuery.getId());
        return createDetailDto(aipQuery, routine);
    }

    public AipQuery findWithUserInitialized(String id) {
        return store.findWithUser(id);
    }

    public List<AipQueryDto> listSavedQueryDtos(String userId) {
        List<AipQuery> all = store.findQueriesOfUser(userId);
        return beanMappingService.mapTo(all, AipQueryDto.class);
    }

    private AipQueryDetailDto createDetailDto(@NonNull AipQuery query, @Nullable ExportRoutine routine) {
        AipQueryDetailDto aipQueryDto = beanMappingService.mapTo(query, AipQueryDetailDto.class);
        if (routine != null) {
            aipQueryDto.setExportRoutine(beanMappingService.mapTo(routine, AipQueryDetailExportRoutineDto.class));
        }
        return aipQueryDto;
    }

    @Transactional
    public void saveAipQuery(String userId, Params params, String queryName) {
        Result<IndexedArclibXmlDocument> allResults = indexArclibXmlStore.findAllIgnorePagination(params);
        params.setPage(0);
        store.save(new AipQuery(new User(userId), allResults, params, queryName));
    }

    public void downloadQueryResult(@NonNull String queryId, @NonNull ExportConfig exportConfig, HttpServletResponse response) throws IOException {
        Set<String> ids;
        if (queryId.equals(BUCKET_QUERY_API_ID)) {
            ids = favoritesBucketService.findFavoriteIdsOfUser(userDetails.getUser().getId());
        } else {
            AipQuery aipQuery = findWithUserInitialized(queryId);
            notNull(aipQuery, () -> new MissingObject(AipQuery.class, queryId));
            ids = aipQuery.getResult().getItems().stream().map(IndexedArclibXmlDocument::getId).collect(Collectors.toSet());
        }
        downloadResult(queryId, exportConfig, ids, response);
    }

    public void downloadBucketResult(@NonNull BucketExportRequestDto reqDto, HttpServletResponse response) throws IOException {
        Set<String> ids = reqDto.getIds().isEmpty() ?
                favoritesBucketService.findFavoriteIdsOfUser(userDetails.getUser().getId())
                : reqDto.getIds();
        downloadResult("bucket", reqDto.getExportConfig(), ids, response);
    }

    public Path exportQueryResult(String queryId, ExportConfig exportConfig, boolean async) throws IOException {
        AipQuery aipQuery = findWithUserInitialized(queryId);
        notNull(aipQuery, () -> new MissingObject(AipQuery.class, queryId));
        Set<String> ids = aipQuery.getResult().getItems().stream().map(IndexedArclibXmlDocument::getId).collect(Collectors.toSet());
        User user = aipQuery.getUser();
        return aipExportService.initiateExport(ids, exportConfig, async, user);
    }

    public Path exportBucketResult(BucketExportRequestDto reqDto) throws IOException {
        Set<String> ids = reqDto.getIds().isEmpty() ?
                favoritesBucketService.findFavoriteIdsOfUser(userDetails.getUser().getId())
                : reqDto.getIds();
        User user = userDetails.getUser();
        return aipExportService.initiateExport(ids, reqDto.getExportConfig(), true, user);
    }

    private void downloadResult(@NonNull String queryId, @NonNull ExportConfig exportConfig, @NonNull Set<String> ids, HttpServletResponse response) throws IOException {
        ExportScope firstExportConfig = exportConfig.getScope().iterator().next();
        boolean zipIt = exportConfig.getScope().size() > 1 || firstExportConfig.getContentType() == null;
        String fileName = "aip_query_" + (zipIt ? queryId + ".zip" : firstExportConfig.getFsName());
        response.setContentType(zipIt ? "application/zip" : firstExportConfig.getContentType());
        response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
        try (OutputStream os = zipIt ? new ZipOutputStream(response.getOutputStream()) : new BufferedOutputStream(response.getOutputStream())) {
            aipExportService.download(ids, exportConfig, os);
        }
    }

    @Transactional
    public void delete(AipQuery entity) {
        store.delete(entity);
    }

    @Transactional
    public void hardDelete(AipQuery entity) {
        store.hardDelete(entity);
    }

    @Autowired
    public void setExportRoutineStore(ExportRoutineStore exportRoutineStore) {
        this.exportRoutineStore = exportRoutineStore;
    }

    @Autowired
    public void setIndexArclibXmlStore(IndexedArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Autowired
    public void setStore(AipQueryStore store) {
        this.store = store;
    }

    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setFavoritesBucketService(FavoritesBucketService favoritesBucketService) {
        this.favoritesBucketService = favoritesBucketService;
    }

    @Autowired
    public void setAipExportService(AipExportService aipExportService) {
        this.aipExportService = aipExportService;
    }
}
