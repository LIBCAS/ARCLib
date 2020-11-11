package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.dto.WorkflowDefinitionDto;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.store.WorkflowDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

@Service
public class WorkflowDefinitionService {
    private WorkflowDefinitionStore store;
    private BeanMappingService beanMappingService;

    public Collection<WorkflowDefinitionDto> listWorkflowDefinitionDtos() {
        Collection<WorkflowDefinition> all = store.findAll();
        return beanMappingService.mapTo(all, WorkflowDefinitionDto.class);
    }

    public WorkflowDefinition find(String id) {
        return store.find(id);
    }

    public Collection<WorkflowDefinition> findAll() {
        return store.findAll();
    }

    @Transactional
    public WorkflowDefinition save(WorkflowDefinition entity) {
        if (entity.getBpmnDefinition() != null && entity.getBpmnDefinition().contains("camunda:jobPriority"))
            throw new ForbiddenException("BPMN definition can't contain job prioritization (camunda:jobPriority)");
        return store.save(entity);
    }

    @Transactional
    public Collection<? extends WorkflowDefinition> save(Collection<? extends WorkflowDefinition> entities) {
        return store.save(entities);
    }

    @Transactional
    public void delete(WorkflowDefinition entity) {
        store.delete(entity);
    }

    @Transactional
    public void hardDelete(WorkflowDefinition entity) {
        store.hardDelete(entity);
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setStore(WorkflowDefinitionStore store) {
        this.store = store;
    }
}
