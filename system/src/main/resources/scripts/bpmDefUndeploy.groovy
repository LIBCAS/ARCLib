package scripts


import cz.cas.lib.arclib.domain.Batch
import cz.cas.lib.arclib.service.BatchService
import cz.cas.lib.arclib.domainbase.exception.GeneralException
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.Deployment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

Logger log = LoggerFactory.getLogger("BPM definition undeploy script")
BatchService batchService = spring.getBean(BatchService.class)
RepositoryService repositoryService = spring.getBean(RepositoryService.class)
TransactionTemplate transactionTemplate = spring.getBean(TransactionTemplate.class)

List<Batch> allDeployed = batchService.findAllDeployed()
log.info("clearing " + allDeployed.size() + " workflow deployments")
List<Batch> stillDeployedBatches = new ArrayList<>()
for (Batch batch : allDeployed) {
    List<Deployment> list = repositoryService.createDeploymentQuery().deploymentName(batch.getId()).list()
    if (list.size() != 1)
        throw new GeneralException("couldn't delete bpmn deployment used with batch " + batch.getId() + " .. expected to find 1 deployment but found " + list.size())
    try {
        repositoryService.deleteDeployment(list.get(0).getId(), false)
    } catch (Exception e) {
        stillDeployedBatches.add(batch)
        continue
    }
    batch.setBpmDefDeployed(false)
    transactionTemplate.execute(new TransactionCallback<Batch>() {
        @Override
        Batch doInTransaction(TransactionStatus status) {
            return batchService.save(batch)
        }
    })
}
log.info("cleared " + (allDeployed.size() - stillDeployedBatches.size()) + " workflow deployments")
if (!stillDeployedBatches.isEmpty())
    log.warn("failed to clear " + stillDeployedBatches.size() + " batches: " + Arrays.toString(stillDeployedBatches.toArray()))