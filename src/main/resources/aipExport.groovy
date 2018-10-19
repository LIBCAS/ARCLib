import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import cz.cas.lib.arclib.service.AipService
import cz.cas.lib.core.scheduling.job.Job
import cz.cas.lib.core.scheduling.job.JobService

AipService aipService = spring.getBean(AipService.class)
JobService jobService = spring.getBean(JobService.class)

ObjectMapper mapper = new ObjectMapper()
List<String> aipIds = mapper.readValue(aipIdsJson, new TypeReference<List<String>>() {})

aipService.exportMultipleAips(aipIds, Boolean.valueOf(all), exportLocationPath);

Job job = jobService.find(jobId);
job.setActive(false);
jobService.save(job);