package cz.inqool.uas.bpm.workflow;


import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.security.Permissions;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.Api;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

/**
 * Api for interaction with workflows.
 *
 * <p>
 *     User needs to have BPM role.
 * </p>
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@RolesAllowed(Permissions.BPM)
@RestController
@Api(value = "workflow", description = "Api for interaction with workflows")
@RequestMapping("/api/workflows")
public class WorkflowApi {

    private WorkflowList workflowList;

    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Result<WorkflowDto> list() {
        return workflowList.list();
    }

    @Inject
    public void setWorkflowList(WorkflowList workflowList) {
        this.workflowList = workflowList;
    }
}
