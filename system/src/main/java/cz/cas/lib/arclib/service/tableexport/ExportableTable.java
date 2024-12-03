package cz.cas.lib.arclib.service.tableexport;

import java.util.List;
import java.util.stream.Collectors;

public interface ExportableTable {

    Object getExportTableValue(String col);

    default List<Object> getExportTableValues(List<String> columns) {
        return columns.stream().map(this::getExportTableValue).collect(Collectors.toList());
    }
}
