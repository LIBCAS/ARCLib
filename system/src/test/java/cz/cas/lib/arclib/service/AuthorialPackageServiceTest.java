package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.core.sequence.Generator;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.solr.core.SolrTemplate;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.asList;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AuthorialPackageServiceTest extends DbTest {

    private AuthorialPackage authorialPackage = new AuthorialPackage();
    private Sip sip = new Sip();
    private IngestWorkflow ingestWorkflow = new IngestWorkflow();
    private Batch batch = new Batch();

    private AuthorialPackageService service = new AuthorialPackageService();
    private BatchService batchService = new BatchService();
    private IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();

    private SipStore sipStore = new SipStore();
    private AuthorialPackageStore authorialPackageStore = new AuthorialPackageStore();
    private BatchStore batchStore = new IndexMockBatchStore();
    private IngestWorkflowStore ingestWorkflowStore = new IngestWorkflowStore();

    @Mock
    private IndexArclibXmlStore arclibXmlStore;
    @Mock
    private SolrTemplate solrTemplate;
    @Mock
    private Generator generator;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setSip(sip);
        sip.setAuthorialPackage(authorialPackage);
        batch.setDebuggingModeActive(true);

        service.setAuthorialPackageStore(authorialPackageStore);
        service.setBatchService(batchService);
        batchService.setDelegate(batchStore);
        batchStore.setTemplate(solrTemplate);
        service.setIngestWorkflowService(ingestWorkflowService);
        ingestWorkflowService.setStore(ingestWorkflowStore);
        ingestWorkflowStore.setGenerator(generator);
        service.setSipStore(sipStore);
        service.setIndexArclibXmlStore(arclibXmlStore);

        initializeStores(ingestWorkflowStore, batchStore, sipStore, authorialPackageStore);
        authorialPackageStore.save(authorialPackage);
        sipStore.save(sip);
        batchStore.save(batch);
        ingestWorkflowStore.save(ingestWorkflow);
    }

    @Test
    public void forgetWithBatch() {
        service.forgetAuthorialPackage(authorialPackage.getId());
        assertThat(sipStore.findAny(), nullValue());
        assertThat(batchStore.findAny(), nullValue());
        assertThat(ingestWorkflowStore.findAny(), nullValue());
        assertThat(authorialPackageStore.findAny(), nullValue());
    }

    @Test
    public void forgetWithoutBatch() {
        IngestWorkflow iw2 = new IngestWorkflow();
        iw2.setBatch(batch);
        ingestWorkflowStore.save(iw2);
        service.forgetAuthorialPackage(authorialPackage.getId());
        assertThat(sipStore.findAny(), nullValue());
        assertThat(batchStore.findAny().getDeleted(), nullValue());
        assertThat(ingestWorkflowStore.find(ingestWorkflow.getId()), nullValue());
        assertThat(authorialPackageStore.findAny(), nullValue());
    }

    @Test
    public void forgotMultipleVersions() {
        IngestWorkflow iw2 = new IngestWorkflow();
        iw2.setSip(sip);
        Batch batch2 = new Batch();
        batch2.setDebuggingModeActive(true);
        iw2.setBatch(batch2);

        Sip sip2 = new Sip();
        sip2.setAuthorialPackage(authorialPackage);
        IngestWorkflow iw3 = new IngestWorkflow();
        iw3.setSip(sip2);
        Batch batch3 = new Batch();
        batch3.setDebuggingModeActive(true);
        iw3.setBatch(batch3);

        sipStore.save(sip2);
        batchStore.save(asList(batch2, batch3));
        ingestWorkflowStore.save(asList(iw2, iw3));

        service.forgetAuthorialPackage(authorialPackage.getId());

        assertThat(sipStore.findAll(), hasSize(0));
        assertThat(ingestWorkflowStore.findAll(), hasSize(0));
        assertThat(batchStore.findAll(), hasSize(0));
        assertThat(authorialPackageStore.findAny(), nullValue());
    }

    @Test
    public void forgetMixedIngests() {
        IngestWorkflow iw2 = new IngestWorkflow();
        iw2.setSip(sip);
        Batch batch2 = new Batch();
        batch2.setDebuggingModeActive(true);
        iw2.setBatch(batch2);

        Sip sip2 = new Sip();
        sip2.setAuthorialPackage(authorialPackage);
        IngestWorkflow iw3 = new IngestWorkflow();
        iw3.setSip(sip2);
        Batch batch3 = new Batch();
        batch3.setDebuggingModeActive(false);
        iw3.setBatch(batch3);

        sipStore.save(sip2);
        batchStore.save(asList(batch2, batch3));
        ingestWorkflowStore.save(asList(iw2, iw3));

        assertThrown(() -> service.forgetAuthorialPackage(authorialPackage.getId())).isInstanceOf(IllegalArgumentException.class);

        Collection<Sip> sips = sipStore.findAll();
        assertThat(sips, hasSize(2));
        for (Sip s : sips) {
            assertThat(s.getDeleted(), nullValue());
        }
        Collection<IngestWorkflow> ingestWorkflows = ingestWorkflowStore.findAll();
        assertThat(ingestWorkflows, hasSize(3));
        for (IngestWorkflow s : ingestWorkflows) {
            assertThat(s.getDeleted(), nullValue());
        }
        Collection<Batch> batches = batchStore.findAll();
        assertThat(batches, hasSize(3));
        for (Batch s : batches) {
            assertThat(s.getDeleted(), nullValue());
        }
        assertThat(authorialPackageStore.findAny().getDeleted(), nullValue());
    }

    private class IndexMockBatchStore extends BatchStore {
        @Override
        public Batch index(Batch obj) {
            return obj;
        }

        @Override
        public Collection<? extends Batch> index(Collection<? extends Batch> objects) {
            return objects;
        }
    }
}
