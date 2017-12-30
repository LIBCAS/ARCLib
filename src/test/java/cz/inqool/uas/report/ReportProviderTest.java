package cz.inqool.uas.report;

import cz.inqool.uas.report.provider.ReportProvider;
import cz.inqool.uas.report.provider.ReportProviders;
import org.junit.Test;

import java.util.Map;

import static cz.inqool.uas.util.Utils.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReportProviderTest {

    @Test
    public void simpleTest() {
        ReportProvider provider = new ReportProvider() {
            @Override
            public Map<String, Object> provide(Map<String, Object> input) {
                return null;
            }

            @Override
            public String getName() {
                return "Name";
            }
        };

        ReportProviders providers = new ReportProviders();
        providers.setProviders(asList(provider));

        assertThat(providers.getProviders(), hasSize(1));
        assertThat(providers.getProviders(), containsInAnyOrder(provider));
        assertThat(providers.isValid(provider.getClass().getName()), is(true));
        assertThat(providers.getProvider(provider.getClass().getName()), is(provider));
    }

    @Test
    public void nonExistingTest() {
        ReportProvider nonProvider = new ReportProvider() {
            @Override
            public Map<String, Object> provide(Map<String, Object> input) {
                return null;
            }

            @Override
            public String getName() {
                return "Name";
            }
        };

        ReportProviders providers = new ReportProviders();
        providers.setProviders(asList());

        assertThat(providers.getProviders(), hasSize(0));
        assertThat(providers.isValid(nonProvider.getClass().getName()), is(false));
        assertThat(providers.getProvider(nonProvider.getClass().getName()), is(nullValue()));
    }

    @Test
    public void combinedTest() {
        ReportProvider provider = new ReportProvider() {
            @Override
            public Map<String, Object> provide(Map<String, Object> input) {
                return null;
            }

            @Override
            public String getName() {
                return "Name";
            }
        };

        ReportProvider nonProvider = new ReportProvider() {
            @Override
            public Map<String, Object> provide(Map<String, Object> input) {
                return null;
            }

            @Override
            public String getName() {
                return "Name 2";
            }
        };

        ReportProviders providers = new ReportProviders();
        providers.setProviders(asList(provider));

        assertThat(providers.getProviders(), hasSize(1));
        assertThat(providers.getProviders(), containsInAnyOrder(provider));
        assertThat(providers.isValid(provider.getClass().getName()), is(true));
        assertThat(providers.getProvider(provider.getClass().getName()), is(provider));

        assertThat(providers.isValid(nonProvider.getClass().getName()), is(false));
        assertThat(providers.getProvider(nonProvider.getClass().getName()), is(nullValue()));
    }
}
