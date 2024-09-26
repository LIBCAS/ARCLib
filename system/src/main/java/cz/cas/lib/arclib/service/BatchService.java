package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.BatchDetailDto;
import cz.cas.lib.arclib.dto.BatchDetailIngestWorkflowDto;
import cz.cas.lib.arclib.dto.BatchDto;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class BatchService implements DelegateAdapter<Batch> {

    @Getter
    private BatchStore delegate;
    private BeanMappingService beanMappingService;


    @Transactional
    public Batch create(Batch b, String userId) {
        return delegate.create(b, userId);
    }

    /**
     * Gets list of batches respecting the <code>params</code>
     *
     * @param params params for filtering
     * @return result containing the list of batches
     */
    public Result<Batch> getBatches(Params params) {
        return delegate.findAll(params);
    }

    public Result<BatchDto> listBatchDtos(Params params) {
        Result<Batch> all = delegate.findAll(params);
        List<BatchDto> allAsDtos = beanMappingService.mapTo(all.getItems(), BatchDto.class);

        Result<BatchDto> result = new Result<>();
        result.setItems(allAsDtos);
        result.setCount(all.getCount());
        return result;
    }

    public List<Batch> findAllDeployed() {
        return delegate.findAllDeployed();
    }

    /**
     * Gets batch together with the associated ingest workflows.
     *
     * @param id id of the batch
     * @return batch with the ingest workflows filled
     */
    public Batch get(String id) {
        Batch batch = delegate.findWithIngestWorkflowsFilled(id);
        notNull(batch, () -> new MissingObject(Batch.class, id));
        return batch;
    }

    /**
     * Gets batch together with the associated ingest workflows.
     *
     * @param id id of the batch
     * @return batch with the ingest workflows filled
     */
    public BatchDetailDto getDetailView(String id) {
        Batch batch = delegate.findWithIngestWorkflowsFilled(id);
        notNull(batch, () -> new MissingObject(Batch.class, id));
        BatchDetailDto batchDetailDto = beanMappingService.mapTo(batch, BatchDetailDto.class);
        batchDetailDto.setIngestWorkflows(beanMappingService.mapTo(batch.getIngestWorkflows(), BatchDetailIngestWorkflowDto.class));
        return batchDetailDto;
    }

    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setDelegate(BatchStore delegate) {
        this.delegate = delegate;
    }
}
