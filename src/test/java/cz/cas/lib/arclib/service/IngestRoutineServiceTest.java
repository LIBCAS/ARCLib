package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.store.IngestRoutineStore;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.scheduling.JobRunner;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.scheduling.job.JobStore;
import cz.cas.lib.core.script.ScriptExecutor;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IngestRoutineServiceTest extends SrDbTest {


    private JobService jobService;
    private JobStore jobStore;
    private UserStore userStore;
    private IngestRoutineStore ingestRoutineStore;

    private ProducerProfileStore producerProfileStore;
    private IngestRoutineService ingestRoutineService;
    private JobRunner jobRunner;
    private User user;
    private ScriptExecutor scriptExecutor;
    private UserDelegate userDelegate;
    private String PATH_TO_SCRIPT = "/testScript.groovy";

    @Before
    public void setUp() {
        producerProfileStore = new ProducerProfileStore();
        producerProfileStore.setTemplate(getTemplate());

        jobStore = new JobStore();
        jobStore.setTemplate(getTemplate());

        userStore = new UserStore();
        userStore.setTemplate(getTemplate());

        ingestRoutineStore = new IngestRoutineStore();

        initializeStores(producerProfileStore, jobStore, userStore, ingestRoutineStore);

        user = new User();
        userDelegate = new UserDelegate(user);

        ingestRoutineService = new IngestRoutineService();
        ingestRoutineService.setUserDetails(userDelegate);
        ingestRoutineService.setBatchStartScript(new ClassPathResource(PATH_TO_SCRIPT));
        ingestRoutineService.setProducerProfileStore(producerProfileStore);

        scriptExecutor = new ScriptExecutor();

        jobRunner = new JobRunner();
        jobRunner.setExecutor(scriptExecutor);

        jobService = new JobService();
        jobService.setDelegate(jobStore);
        jobService.setRunner(jobRunner);
        ingestRoutineService.setJobService(jobService);

        ingestRoutineService.setDelegate(ingestRoutineStore);
    }

    @Test
    @Transactional
    @Ignore
    public void saveRutineWithActiveJob() {
        flushCache();

        Job job = new Job();
        job.setActive(true);
        job.setName("test job name");
        job.setTiming("*/30 * * * * ?");

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfileStore.save(producerProfile);

        flushCache();

        IngestRoutine ingestRoutine = new IngestRoutine();
        ingestRoutine.setProducerProfile(producerProfile);
        ingestRoutine.setTransferAreaPath("test transfer area path");
        ingestRoutine.setWorkflowConfig("test workflow config");
        ingestRoutine.setJob(job);

        ingestRoutineService.save(ingestRoutine);

        flushCache();

        job = jobStore.find(job.getId());
        assertThat(job.getScriptType(), is(ScriptType.GROOVY));
    }
}
