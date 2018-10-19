package cz.cas.lib.core.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.config.change.ConfigFile;
import cz.cas.lib.core.config.change.ConfigStore;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ConfigStoreTest extends DbTest {
    protected ConfigStore configStore;

    @Before
    public void setup() throws Exception {

        configStore = new ConfigStore();
        configStore.setEntityManager(getEm());
        configStore.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void getLastTest() throws InterruptedException {
        ConfigFile last = configStore.getLast();
        assertThat(last, is(nullValue()));

        ConfigFile a = new ConfigFile();
        a.setCreated(Instant.now());
        configStore.save(a);

        flushCache();


        ConfigFile b = new ConfigFile();
        b.setCreated(Instant.now().plusMillis(100));
        configStore.save(b);

        flushCache();


        ConfigFile c = new ConfigFile();
        c.setCreated(Instant.now().plusMillis(200));
        configStore.save(c);

        flushCache();

        last = configStore.getLast();

        assertThat(last.getId(), is(c.getId()));
    }
}
