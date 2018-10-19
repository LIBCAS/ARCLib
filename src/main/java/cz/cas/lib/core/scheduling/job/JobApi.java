package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.GeneralApi;
import cz.cas.lib.core.scheduling.JobRunner;
import cz.cas.lib.core.scheduling.run.JobRun;
import cz.cas.lib.core.scheduling.run.JobRunService;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.List;

import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Api for managing jobs.
 */
@RolesAllowed(Roles.JOB)
@RestController
@Api(value = "job", description = "Api for managing jobs")
@RequestMapping("/api/jobs")
public class JobApi implements GeneralApi<Job> {
    private JobRunner runner;

    private JobRunService runService;

    @Getter
    private JobService adapter;

    /**
     * Runs the job now.
     *
     * @param id Id of the {@link Job}
     */
    @Transactional
    @ApiOperation(value = "Runs the job now.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Job not found")})
    @RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
    public void run(@ApiParam(value = "Id of the sequence", required = true)
                    @PathVariable("id") String id) {
        Job job = adapter.find(id);
        runner.run(job);
    }

    /**
     * Gets one instance specified by jobId and runId
     *
     * @param jobId Id of the Job
     * @param runId Id of the JobRun
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @ApiOperation(value = "Gets one instance specified by id", response = DomainObject.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = DomainObject.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{jobId}/runs/{runId}", method = RequestMethod.GET)
    @Transactional
    public JobRun get(
            @ApiParam(value = "Id of the Job", required = true) @PathVariable("jobId") String jobId,
            @ApiParam(value = "Id of the JobRun", required = true) @PathVariable("runId") String runId
    ) {
        JobRun entity = runService.find(jobId, runId);
        notNull(entity, () -> new MissingObject(getAdapter().getType(), runId));

        return entity;
    }

    /**
     * Gets runs of specified Job that respect the selected {@link Params}.
     *
     * <p>
     * Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     * see {@link Params}.
     * </p>
     * <p>
     * Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param id     Id of the Job instance
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets runs of specified Job that respect the selected parameters", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{id}/runs", method = RequestMethod.GET)
    @Transactional
    public Result<JobRun> listRuns(
            @ApiParam(value = "Id of the existing Job", required = true) @PathVariable("id") String id,
            @ApiParam(value = "Parameters to comply with", required = true) @ModelAttribute Params params
    ) {
        return runService.findAll(id, params);
    }

    @Inject
    public void setAdapter(JobService service) {
        this.adapter = service;
    }

    @Inject
    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }

    @Inject
    public void setRunService(JobRunService runService) {
        this.runService = runService;
    }
}
