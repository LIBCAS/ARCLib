package cz.inqool.uas.scheduling;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.scheduling.run.JobRun;
import cz.inqool.uas.scheduling.run.JobRunStore;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.inqool.uas.util.Utils.notNull;

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
