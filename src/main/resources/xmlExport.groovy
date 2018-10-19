import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cas.lib.arclib.service.AipService
import cz.cas.lib.core.scheduling.job.Job
import cz.cas.lib.core.scheduling.job.JobService

AipService aipService = spring.getBean(AipService.class)
JobService jobService = spring.getBean(JobService.class)

TypeReference<HashMap<String, List<Integer>>> typeRef = new TypeReference<HashMap<String, List<Integer>>>() {}
ObjectMapper mapper = new ObjectMapper()
Map<String, List<Integer>> aipIdsAndVersions = mapper.readValue(aipIdsAndVersionsJson, typeRef)

aipService.exportMultipleXmls(aipIdsAndVersions, exportLocationPath)

Job job = jobService.find(jobId)
job.setActive(false)
jobService.save(job)
