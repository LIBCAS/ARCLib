package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.dto.WorkflowDefinitionDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.WorkflowDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class WorkflowDefinitionService {
    private WorkflowDefinitionStore store;
    private BeanMappingService beanMappingService;
    private UserDetails userDetails;

    public WorkflowDefinition find(String id) {
        return store.find(id);
    }

    @Transactional
    public WorkflowDefinition save(WorkflowDefinition entity) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            entity.setProducer(new Producer(userDetails.getProducerId()));
        } else {
            notNull(entity.getProducer(), () -> new BadRequestException("WorkflowDefinition has to have producer assigned"));
        }

        if (entity.getBpmnDefinition() != null && entity.getBpmnDefinition().contains("camunda:jobPriority"))
            throw new ForbiddenException("BPMN definition can't contain job prioritization (camunda:jobPriority)");
        return store.save(entity);
    }

    public Collection<WorkflowDefinitionDto> listWorkflowDefinitionDtos() {
        Collection<WorkflowDefinition> all = this.findFilteredByProducer();
        return beanMappingService.mapTo(all, WorkflowDefinitionDto.class);
    }

    public Collection<WorkflowDefinition> findFilteredByProducer() {
        if (hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
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

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

}
