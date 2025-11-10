package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AipBulkDeletion;
import cz.cas.lib.arclib.domain.packages.AipBulkDeletionState;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.dto.AipBulkDeletionCreateDto;
import cz.cas.lib.arclib.dto.AipBulkDeletionDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.exception.ReingestInProgressException;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipBulkDeletionStore;
import cz.cas.lib.arclib.store.ReingestStore;
import cz.cas.lib.arclib.store.SipStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;

@Slf4j
@Service
public class AipBulkDeletionService {

    private SipStore sipStore;
    private TransactionTemplate transactionTemplate;
    private AipService aipService;
    private IngestWorkflowService ingestWorkflowService;
    private UserDetails userDetails;
    private AipBulkDeletionStore store;
    private BeanMappingService beanMappingService;
    private ReingestStore reingestStore;

    public void bulkDelete(AipBulkDeletionCreateDto request) throws ReingestInProgressException {
        if (ObjectUtils.isEmpty(request.getAipIds())) {
            return;
        }
        if (reingestStore.getCurrent() != null) {
            throw new ReingestInProgressException("bulk deletion not allowed when reingest is running");
        }
        Set<String> aipIds = Arrays.stream(request.getAipIds().split(",")).map(s -> s.trim()).collect(Collectors.toSet());

        AipBulkDeletion entity = new AipBulkDeletion();
        entity.setAipIds(aipIds);
        entity.setDeleteIfNewerVersionsDeleted(request.isDeleteIfNewerVersionsDeleted());
        entity.setState(AipBulkDeletionState.RUNNING);
        entity.setProducer(userDetails.getUser().getProducer());
        entity.setCreator(userDetails.getUser());
        MutableInt deletedCount = new MutableInt(0);
        transactionTemplate.execute(t -> store.save(entity));

        boolean checkProducer = !hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE);

        new Thread(() -> {
            try {
                for (String aipId : aipIds) {
                    transactionTemplate.execute(t -> {
                        Sip sipToDelete = sipStore.find(aipId);
                        if (sipToDelete.isLatestVersion()) {
                            log.info("skipping deletion od aip {} since that is a latest data version", aipId);
                            return null;
                        } else {
                            List<IngestWorkflow> allIngestWorkflows = ingestWorkflowService.findByAuthorialPackageId(sipToDelete.getAuthorialPackage().getId());

                            if (allIngestWorkflows.stream().anyMatch(IngestWorkflow::wasIngestedInDebugMode)) {
                                log.info("skipping deletion od aip {} since its ingests were done in debug mode", aipId);
                                return null;
                            }

                            if (checkProducer) {
                                allIngestWorkflows.forEach(iw -> eq(iw.getProducerProfile().getProducer(), entity.getProducer(), () -> new ForbiddenOperation("user " + entity.getCreator() + " can't delete aip " + aipId + " since it does belong to other producer")));
                            }

                            boolean newerPersistedSipExists = allIngestWorkflows.stream().anyMatch(
                                    i -> i.getProcessingState() == IngestWorkflowState.PERSISTED && i.getSip().getVersionNumber() > sipToDelete.getVersionNumber()
                            );
                            if (!newerPersistedSipExists && !request.isDeleteIfNewerVersionsDeleted()) {
                                log.info("skipping deletion od aip {} since its newer data version is deleted", aipId);
                                return null;
                            } else {
                                try {
                                    aipService.changeAipState(aipId, IndexedAipState.DELETED, false);
                                } catch (AipStateChangeException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                                entity.setDeletedCount(deletedCount.incrementAndGet());
                                store.save(entity);
                            }
                        }
                        return null;
                    });
                }
            } catch (Exception e) {
                entity.setState(AipBulkDeletionState.FAILED);
                transactionTemplate.execute(t -> store.save(entity));
                log.error("bulk deletion: " + entity + " failed", e);
                return;
            }

            entity.setState(AipBulkDeletionState.FINISHED);
            transactionTemplate.execute(t -> store.save(entity));
        }).start();
    }

    public Collection<AipBulkDeletionDto> listAipBulkDeletionDtos() {
        Collection<AipBulkDeletion> all = this.findFilteredByProducer();
        return beanMappingService.mapTo(all, AipBulkDeletionDto.class);
    }

    public Collection<AipBulkDeletion> findFilteredByProducer() {
        if (hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }

    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setStore(AipBulkDeletionStore store) {
        this.store = store;
    }

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Autowired
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Autowired
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setReingestStore(ReingestStore reingestStore) {
        this.reingestStore = reingestStore;
    }
}
