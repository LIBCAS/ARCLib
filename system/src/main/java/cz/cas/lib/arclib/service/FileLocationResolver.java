package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.reingest.Reingest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileLocationResolver {

    private Path fileStorage;
    private Path workspace;

    public Path getReingestFolder(Reingest reingest) {
        return fileStorage.resolve("reingest").resolve(reingest.getId());
    }

    public Path getReingestProducerProfileFolder(Reingest reingest, ProducerProfile producerProfile) {
        return getReingestFolder(reingest).resolve(producerProfile.getId());
    }

    public Path getReingestWorkflowConfigFolder(Reingest reingest, ProducerProfile producerProfile, String workflowConfigChecksum) {
        return getReingestProducerProfileFolder(reingest, producerProfile).resolve(workflowConfigChecksum);
    }

    public Path getReingestWorkflowConfigFile(Reingest reingest, ProducerProfile producerProfile, String workflowConfigChecksum) {
        return getReingestWorkflowConfigFolder(reingest, producerProfile, workflowConfigChecksum).resolve("config.json");
    }

    public Path getReingestWorkflowConfigSkipFile(Reingest reingest, ProducerProfile producerProfile, String workflowConfigChecksum) {
        return getReingestWorkflowConfigFolder(reingest, producerProfile, workflowConfigChecksum).resolve("config.json.skip");
    }

    public Path getWorkspaceFolder() {
        return workspace;
    }

    @Autowired
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = Paths.get(workspace);
    }

    @Autowired
    public void setFileStorage(@Value("${arclib.path.fileStorage}") String fileStorage) {
        this.fileStorage = Paths.get(fileStorage);
    }
}
