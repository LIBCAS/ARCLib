package scripts


import cz.cas.lib.arclib.domain.export.ExportRoutine
import cz.cas.lib.arclib.service.AipQueryService
import cz.cas.lib.arclib.service.ExportRoutineService
import cz.cas.lib.core.scheduling.job.Job
import cz.cas.lib.core.scheduling.job.JobService
import org.springframework.transaction.support.TransactionTemplate

AipQueryService aipQueryService = spring.getBean(AipQueryService.class)
JobService jobService = spring.getBean(JobService.class)
ExportRoutineService exportRoutineService = spring.getBean(ExportRoutineService.class)
TransactionTemplate tt = spring.getBean(TransactionTemplate.class)

ExportRoutine exportRoutine = exportRoutineService.findByAipQueryId(aipQueryId)
aipQueryService.exportQueryResult(aipQueryId, exportRoutine.getConfig(), false)
Job job = jobService.find(jobId)
job.setActive(false)
tt.execute({ t -> jobService.save(job) })