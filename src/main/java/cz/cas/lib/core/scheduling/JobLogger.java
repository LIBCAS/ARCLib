package cz.cas.lib.core.scheduling;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.scheduling.run.JobRun;
import cz.cas.lib.core.scheduling.run.JobRunStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class JobLogger {
    private JobRunStore store;

    @Transactional
    public void log(JobRun run) {
        notNull(run, () -> new BadArgument("run"));

        store.save(run);
    }

    @Inject
    public void setStore(JobRunStore store) {
        this.store = store;
    }
}
