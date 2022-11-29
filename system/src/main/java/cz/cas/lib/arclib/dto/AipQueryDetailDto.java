package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AipQueryDetailDto {
    private String id;
    private String name;
    private Instant created;
    private Instant updated;
    private Params query;
    private Result<IndexedArclibXmlDocument> result;
    private AipQueryDetailExportRoutineDto exportRoutine;
}
