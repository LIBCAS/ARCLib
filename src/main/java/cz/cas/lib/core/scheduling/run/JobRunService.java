package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.asList;

@Service
public class JobRunService {
    private JobRunStore store;

    public Result<JobRun> findAll(String jobId, Params params) {
        Filter filter = new Filter("job.id", FilterOperation.EQ, jobId, null);
        params.setFilter(asList(params.getFilter(), filter));

        return store.findAll(params);
    }

    public JobRun find(String jobId, String runId) {
        return store.find(jobId, runId);
    }

    @Inject
    public void setStore(JobRunStore store) {
        this.store = store;
    }
}
