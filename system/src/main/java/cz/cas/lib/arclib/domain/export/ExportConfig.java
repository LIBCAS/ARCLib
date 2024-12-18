package cz.cas.lib.arclib.domain.export;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domainbase.util.ArrayJsonConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Embeddable
@Getter
@Setter
public class ExportConfig {

    @Convert(converter = ExportScopeConverter.class)
    @NotEmpty
    private List<ExportScope> scope = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private IdentifierExportType idExportType = IdentifierExportType.XML_ID;

    @Embedded
    private DataReduction dataReduction;

    private boolean generateInfoFile;

    private String exportFolder;

    @Convert(converter = ArrayJsonConverter.class)
    private List<String> metadataSelection;

    @JsonIgnore
    public List<String> getMetadataSelectionNullSafe() {
        return metadataSelection == null ? ALL_METADATA : metadataSelection;
    }

    private static final List<String> ALL_METADATA = List.of(
            "id",
            "authorial_id",
            "label",
            "type",
            "created",
            "updated",
            "aip_state",
            "producer_name",
            "user_name",
            "sip_id",
            "producer_profile",
            "sip_profile",
            "validation_profile",
            "workflow_definition",
            "sip_version_number",
            "sip_version_of",
            "xml_version_number",
            "xml_version_of",
            "latest",
            "dublin_core",
            "arc_event",
            "creating_application",
            "identified_format",
            "img_metadata",
            "premis_event",
            "extracted_format"
    );
}
