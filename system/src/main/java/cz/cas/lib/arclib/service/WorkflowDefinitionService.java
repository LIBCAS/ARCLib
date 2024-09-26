package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.dto.WorkflowDefinitionDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.WorkflowDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class WorkflowDefinitionService {
    private WorkflowDefinitionStore store;
    private BeanMappingService beanMappingService;
    private UserDetails userDetails;

    public WorkflowDefinition find(String id) {
        return store.find(id);
    }

    public WorkflowDefinition findWithDeletedFilteringOff(String id) {
        return store.findWithDeletedFilteringOff(id);
    }

    @Transactional
    public WorkflowDefinition save(WorkflowDefinition entity) {
        notNull(entity.getProducer(), () -> new BadRequestException("WorkflowDefinition must have producer assigned"));
        // if user is not SUPER_ADMIN then entity must be assigned to user's producer
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            eq(entity.getProducer().getId(), userDetails.getUser().getProducer().getId(), () -> new ForbiddenOperation("Producer of WorkflowDefinition must be the same as producer of logged-in user."));
        }

        if (entity.getBpmnDefinition() != null && entity.getBpmnDefinition().contains("camunda:jobPriority"))
            throw new ForbiddenException("BPMN definition can't contain job prioritization (camunda:jobPriority)");

        WorkflowDefinition workflowDefinitionFound = store.find(entity.getId());
        if (workflowDefinitionFound != null) {
            // if user is not SUPER_ADMIN then change of producer is forbidden
            if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
                eq(entity.getProducer().getId(), workflowDefinitionFound.getProducer().getId(), () -> new ForbiddenOperation("Cannot change WorkflowDefinition's Producer"));
            }
            if (!workflowDefinitionFound.isEditable()) {
                throw new ForbiddenOperation(WorkflowDefinition.class, entity.getId());
            }
        }

        entity.setEditable(true);
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


    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setStore(WorkflowDefinitionStore store) {
        this.store = store;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

}
