package cz.inqool.uas.report;

import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.location.ReportLocations;
import org.junit.Test;

import static cz.inqool.uas.util.Utils.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReportLocationTest {


    @Test
    public void simpleTest() {
        ReportLocation location = new ReportLocation() {
            @Override
            public String getId() {
                return "existing";
            }

            @Override
            public String getName() {
                return "Existing location";
            }
        };

        ReportLocations locations = new ReportLocations();
        locations.setLocations(asList(location));

        assertThat(locations.getLocations(), hasSize(1));
        assertThat(locations.getLocations(), containsInAnyOrder(location));
        assertThat(locations.isValid(location.getId()), is(true));
        assertThat(locations.getLocation(location.getId()), is(location));
    }

    @Test
    public void nonExistingTest() {
        ReportLocation nonLocation = new ReportLocation() {
            @Override
            public String getId() {
                return "nonExisting";
            }

            @Override
            public String getName() {
                return "NonExisting location";
            }
        };

        ReportLocations locations = new ReportLocations();
        locations.setLocations(asList());

        assertThat(locations.getLocations(), hasSize(0));
        assertThat(locations.isValid(nonLocation.getId()), is(false));
        assertThat(locations.getLocation(nonLocation.getId()), is(nullValue()));
    }

    @Test
    public void combinedTest() {
        ReportLocation location = new ReportLocation() {
            @Override
            public String getId() {
                return "existing";
            }

            @Override
            public String getName() {
                return "Existing location";
            }
        };

        ReportLocation nonLocation = new ReportLocation() {
            @Override
            public String getId() {
                return "nonExisting";
            }

            @Override
            public String getName() {
                return "NonExisting location";
            }
        };

        ReportLocations locations = new ReportLocations();
        locations.setLocations(asList(location));

        assertThat(locations.getLocations(), hasSize(1));
        assertThat(locations.getLocations(), containsInAnyOrder(location));
        assertThat(locations.isValid(location.getId()), is(true));
        assertThat(locations.getLocation(location.getId()), is(location));

        assertThat(locations.isValid(nonLocation.getId()), is(false));
        assertThat(locations.getLocation(nonLocation.getId()), is(nullValue()));
    }
}
