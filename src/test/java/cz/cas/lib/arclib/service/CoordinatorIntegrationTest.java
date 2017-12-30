package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.api.CoordinatorApi;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.domain.SipState;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.inqool.uas.domain.DomainObject;
import helper.ApiTest;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.uas.util.Utils.asSet;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CoordinatorIntegrationTest implements ApiTest {
    @Inject
    private CoordinatorService service;

    @Inject
    private BatchStore batchStore;

    @Inject
    private SipStore sipStore;

    @Inject
    private JmsTemplate template;

    @Inject
    private CoordinatorApi api;

    public static final String SIP_SOURCES = "SIP_packages";

    /**
     * Test of ({@link CoordinatorService#start(String)}) method. The method is passed a path to the test folder containing three empty
     * files. The test asserts that:
     *
     * 1. there is a new instance of batch created and its state is PROCESSING
     * 2. there are three SIP packages created (one for each file) and their state is PROCESSED
     * 3. SIP ids that belong to the batch are the same as the ids of the SIP packages stored in database
     */
    @Test
    public void startTest() throws Exception {
        final String[] result = new String[1];
        mvc(api).perform(post("/api/coordinator/start")
                .content(SIP_SOURCES.getBytes()))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(24000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSED));

        Collection<Sip> allSips = sipStore.findAll();
        assertThat(allSips, hasSize(5));
        allSips.forEach(sip -> {
            assertThat(sip.getState(), Is.is(SipState.PROCESSED));
        });

        List<String> allSipsIds = allSips.stream()
                .map(DomainObject::getId)
                .collect(Collectors.toList());
        Set<String> batchSipIds = batch.getIds();
        assertThat(batchSipIds.toArray(), is(allSipsIds.toArray()));
    }


    /**
     * Test of ({@link CoordinatorService#cancel(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#start(String)}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#cancel(String)}) that cancels the batch
     *
     * The test asserts that:
     * 1. the state of the batch is CANCELED
     */
    @Test
    public void cancelTest() throws Exception {
        final String[] result = new String[1];
        mvc(api).perform(post("/api/coordinator/start")
                .content(SIP_SOURCES.getBytes()))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/cancel"))
                .andExpect(status().is2xxSuccessful());
        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(12000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    /**
     * Test of ({@link CoordinatorService#cancel(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#start(String)}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#suspend(String)}) that suspends the batch
     *
     * 1. the state of the batch is SUSPENDED
     */
    @Test
    public void suspendTest() throws Exception {
        final String[] result = new String[1];

        mvc(api).perform(post("/api/coordinator/start")
                .content(SIP_SOURCES.getBytes()))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/suspend"))
                .andExpect(status().is2xxSuccessful());
        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(12000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.SUSPENDED));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. There are three methods called in a sequence:
     * 1. method ({@link CoordinatorService#start(String}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#suspend(String}) that suspends the batch
     * 3. method ({@link CoordinatorService#resume(String}) that resumes the batch
     *
     * The test asserts that:
     * 1. the batch is in the state PROCESSING
     * 2. there are three SIP packages created (one for each file) and their state is PROCESSED
     * 3. sip ids that belong to the batch are the same as the ids of the sip packages stored in database
     */
    @Test
    public void resumeTest() throws Exception {
        final String[] result = new String[1];

        mvc(api).perform(post("/api/coordinator/start")
                .content(SIP_SOURCES.getBytes()))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/suspend"))
                .andExpect(status().is2xxSuccessful());
        Thread.sleep(6000);

        mvc(api).perform(post("/api/coordinator/" + batchId + "/resume"))
                .andExpect(status().is2xxSuccessful());

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(60000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSED));

        Collection<Sip> allSips = sipStore.findAll();
        assertThat(allSips, hasSize(5));
        allSips.forEach(sip -> {
            assertThat(sip.getState(), is(SipState.PROCESSED));
        });

        List<String> allSipsIds = allSips.stream()
                .map(DomainObject::getId)
                .collect(Collectors.toList());
        Set<String> batchSipIds = batch.getIds();
        assertThat(batchSipIds.toArray(), is(allSipsIds.toArray()));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. The method is passed ID of a batch that:
     * 1. is in the state SUSPENDED
     * 2. has a SIP package in the state PROCESSING
     *
     * The test asserts that:
     * 1. the batch has not resumed (the return value of the method is false)
     * 2. the batch state is SUSPENDED
     */
    @Test
    public void resumeTestSipWithStateProcessing() throws Exception {
        Sip sip = new Sip();
        sip.setState(SipState.PROCESSING);
        sipStore.save(sip);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIds(asSet(sip.getId()));
        batchStore.save(batch);

        final String[] result = new String[1];

        mvc(api).perform(post("/api/coordinator/" + batch.getId() + "/resume"))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        Boolean hasResumed = Boolean.getBoolean(result[0]);

        Thread.sleep(2000);

        assertThat(hasResumed, is(false));
        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.SUSPENDED));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. The method is passed ID of a batch that:
     * 1. is in the state SUSPENDED
     * 2. has no SIP packages in the state PROCESSING, has only a SIP package in the state NEW
     *
     * The test asserts that:
     * 1. the batch has successfuly resumed (the return value of the method is true)
     * 2. the batch state is PROCESSING
     */
    @Test
    public void resumeTestNoSipWithStateProcessing() throws Exception {
        Sip sip = new Sip();
        sip.setState(SipState.NEW);
        sipStore.save(sip);

        Sip sip2 = new Sip();
        sip2.setState(SipState.PROCESSED);
        sipStore.save(sip2);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIds(asSet(sip.getId(), sip2.getId()));
        batchStore.save(batch);

        final String[] result = new String[1];

        mvc(api).perform(post("/api/coordinator/" + batch.getId() + "/resume"))
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        Boolean hasResumed = Boolean.valueOf(result[0]);

        Thread.sleep(2000);

        assertThat(hasResumed, is(true));
        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.PROCESSED));

        sip = sipStore.find(sip.getId());
        assertThat(sip.getState(), is(SipState.PROCESSED));

        sip2 = sipStore.find(sip2.getId());
        assertThat(sip2.getState(), is(SipState.PROCESSED));
    }

    @After
    public void testTearDown() throws SQLException {
        sipStore.findAll().forEach(sipStore::delete);

        batchStore.findAll().forEach(batchStore::delete);
    }
}
