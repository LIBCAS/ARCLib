package cz.cas.lib.core.scheduling;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.core.scheduling.run.JobRun;
import cz.cas.lib.core.scheduling.run.JobRunStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class JobLogger {
    private JobRunStore store;

    @Transactional
    public void log(JobRun run) {
        notNull(run, () -> new BadArgument("run"));

        store.save(run);
    }

    @Autowired
    public void setStore(JobRunStore store) {
        this.store = store;
    }
}
