import cz.cas.lib.arclib.domain.packages.AuthorialPackageUpdateLock
import cz.cas.lib.arclib.store.AuthorialPackageUpdateLockStore
import cz.cas.lib.core.scheduling.job.Job
import cz.cas.lib.core.scheduling.job.JobService

import java.time.Instant

AuthorialPackageUpdateLockStore authorialPackageUpdateLockStore = spring.getBean(AuthorialPackageUpdateLockStore.class);
JobService jobService = spring.getBean(JobService.class);
AuthorialPackageUpdateLock authorialPackageUpdateLock = authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
Instant latestLockedInstant = authorialPackageUpdateLock.getLatestLockedInstant();
long timePadding = 2l;

if (latestLockedInstant != null && latestLockedInstant.plusSeconds(Long.valueOf(keepAliveUpdateTimeout))
        .isBefore(Instant.now().plusSeconds(timePadding))) {
    authorialPackageUpdateLock.setLocked(false);
    authorialPackageUpdateLockStore.save(authorialPackageUpdateLock);
    Job timeoutCheckJob = authorialPackageUpdateLock.getTimeoutCheckJob();
    timeoutCheckJob.setActive(false);
    jobService.save(timeoutCheckJob);
}
