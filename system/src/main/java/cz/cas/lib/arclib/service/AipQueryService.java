package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.ExportRoutine;
import cz.cas.lib.arclib.dto.AipQueryDto;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ExportRoutineStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AipQueryService {
    private AipQueryStore store;

    private ExportRoutineStore exportRoutineStore;

    public AipQuery find(String id) {
        return store.find(id);
    }

    public List<AipQueryDto> listSavedQueryDtos(String userId) {
        List<AipQuery> all = store.findQueriesOfUser(userId);
        return all.stream()
                .map(query -> {
                    ExportRoutine exportRoutine = exportRoutineStore.findByAipQueryId(query.getId());
                    Instant exportedTime = exportRoutine == null ? null : exportRoutine.getExportTime();
                    String exportLocationPath = exportRoutine == null ? null : exportRoutine.getExportLocationPath();
                    String exportRoutineId = exportRoutine == null ? null : exportRoutine.getId();
                    return new AipQueryDto(query.getId(), query.getName(), query.getCreated(), query.getUpdated(), exportRoutineId, exportedTime,
                            exportLocationPath);
                })
                .collect(Collectors.toList());
    }

    public List<AipQuery> findQueriesOfUser(String userId) {
        return store.findQueriesOfUser(userId);
    }

    @Transactional
    public AipQuery save(AipQuery entity) {
        return store.save(entity);
    }

    @Transactional
    public Collection<? extends AipQuery> save(Collection<? extends AipQuery> entities) {
        return store.save(entities);
    }

    @Transactional
    public void delete(AipQuery entity) {
        store.delete(entity);
    }

    @Transactional
    public void hardDelete(AipQuery entity) {
        store.hardDelete(entity);
    }

    @Inject
    public void setExportRoutineStore(ExportRoutineStore exportRoutineStore) {
        this.exportRoutineStore = exportRoutineStore;
    }

    @Inject
    @Transactional
    public void setStore(AipQueryStore store) {
        this.store = store;
    }
}
