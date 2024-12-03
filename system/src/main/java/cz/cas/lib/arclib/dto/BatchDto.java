package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.service.tableexport.ExportableTable;
import cz.cas.lib.arclib.service.tableexport.TableDataType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDto implements ExportableTable {
    private String id;
    private Producer producer;
    private String transferAreaPath;
    private Instant created;
    private Instant updated;
    private boolean pendingIncidents;
    @Enumerated(EnumType.STRING)
    private BatchState state;
    private ProducerProfileDto producerProfile;
    private SipProfileDto initialSipProfile;
    private ValidationProfileDto initialValidationProfile;
    private WorkflowDefinitionDto initialWorkflowDefinition;

    @Override
    public Object getExportTableValue(String col) {
        return switch (col) {
            case "id" -> id;
            case "producerName" -> producer == null ? null : producer.getName();
            case "transferAreaPath" -> transferAreaPath;
            case "created" -> created;
            case "updated" -> updated;
            case "state" -> state;
            case "pendingIncidents" -> pendingIncidents;
            case "producerProfile" -> producerProfile == null ? null : producerProfile.getName();
            case "initialSipProfile" -> initialSipProfile == null ? null : initialSipProfile.getName();
            case "initialValidationProfile" ->
                    initialValidationProfile == null ? null : initialValidationProfile.getName();
            case "initialWorkflowDefinition" ->
                    initialWorkflowDefinition == null ? null : initialWorkflowDefinition.getName();
            default -> null;
        };
    }

    public static List<TableDataType> getExportTableConfig(List<String> columns) {
        return columns.stream().map(BatchDto::getExportTableConfig).collect(Collectors.toList());
    }

    private static TableDataType getExportTableConfig(String col) {
        return switch (col) {
            case "id", "pendingIncidents" -> TableDataType.OTHER;
            case "producerName", "initialWorkflowDefinition", "initialValidationProfile", "transferAreaPath", "state",
                 "producerProfile", "initialSipProfile" -> TableDataType.STRING_AUTO_SIZE;
            case "created", "updated" -> TableDataType.DATE_TIME;
            default -> throw new UnsupportedOperationException("unsupported export column: " + col);
        };
    }
}
