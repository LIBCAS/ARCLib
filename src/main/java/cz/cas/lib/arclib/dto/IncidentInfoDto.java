package cz.cas.lib.arclib.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@Getter
@Setter
public class IncidentInfoDto {
    private String id;
    private Instant created;
    private Instant ended;
    private String message;
    private String stackTrace;
    private String activityId;
    private String batchId;
    private String externalId;
    private String assignee;
    /**
     * config which was used when incident occurred i.e. config which caused the incident not the one which was used to solve it
     */
    private String config;

    private String processInstanceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncidentInfoDto that = (IncidentInfoDto) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
