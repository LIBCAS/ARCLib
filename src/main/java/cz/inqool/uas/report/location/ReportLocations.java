package cz.inqool.uas.report.location;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

@Service
public class ReportLocations {
    private List<ReportLocation> locations;

    /**
     * Gets all {@link ReportLocation}s.
     *
     * @return {@link List} of {@link ReportLocation}
     */
    public List<ReportLocation> getLocations() {
        return unmodifiableList(locations);
    }

    /**
     * Gets {@link ReportLocation} corresponding to specified id.
     *
     * @param locationId Specified id
     * @return {@link ReportLocation}
     */
    public ReportLocation getLocation(String locationId) {
        return locations.stream()
                        .filter(location -> Objects.equals(location.getId(), locationId))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Tests if there is {@link ReportLocation} with specified id.
     *
     * @param locationId Specified id to test
     * @return Existence of {@link ReportLocation}
     */
    public boolean isValid(String locationId) {
        return locations.stream()
                 .map(ReportLocation::getId)
                 .anyMatch(id -> Objects.equals(id, locationId));
    }

    @Autowired(required = false)
    public void setLocations(List<ReportLocation> locations) {
        this.locations = locations != null ? locations : emptyList();
    }
}
