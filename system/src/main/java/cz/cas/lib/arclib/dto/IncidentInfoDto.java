package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.service.tableexport.ExportableTable;
import cz.cas.lib.arclib.service.tableexport.TableDataType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class IncidentInfoDto implements ExportableTable {
    private String id;
    private Instant created;
    private Instant ended;
    private String message;
    private String stackTrace;
    private String activityId;
    private String batchId;
    private String externalId;
    private User responsiblePerson;
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

    @Override
    public Object getExportTableValue(String col) {
        return switch (col) {
            case "created" -> created;
            case "externalId" -> externalId;
            case "activityId" -> activityId;
            case "responsiblePerson" -> responsiblePerson == null ? null : responsiblePerson.getUsername();
            default -> null;
        };
    }

    public static List<TableDataType> getExportTableConfig(List<String> columns) {
        return columns.stream().map(IncidentInfoDto::getExportTableConfig).collect(Collectors.toList());
    }

    private static TableDataType getExportTableConfig(String col) {
        return switch (col) {
            case "externalId", "activityId", "responsiblePerson" -> TableDataType.STRING_AUTO_SIZE;
            case "created" -> TableDataType.DATE_TIME;
            default -> throw new UnsupportedOperationException("unsupported export column: " + col);
        };
    }
}
