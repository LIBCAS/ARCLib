package cz.cas.lib.arclib.service.tableexport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TableExporter {

    private ObjectMapper objectMapper;

    public void exportCsv(String name, List<String> header, List<List<Object>> table, OutputStream outputStream) {
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream),
                '|',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.NO_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(new String[]{name});
            writer.writeNext(header.toArray(new String[0]));
            for (List<Object> row : table) {
                String[] parsedRow = row.stream().map(this::objectMapperToString).toArray(String[]::new);
                writer.writeNext(parsedRow);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void exportXlsx(String name, List<String> header, @Nullable List<TableDataType> columnTypes, List<List<Object>> table, boolean format, OutputStream outputStream) {
        if (columnTypes != null && header.size() != columnTypes.size()) {
            throw new IllegalArgumentException("if columnTypes are specified length of the list must be the same as length of the header");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Map<TableDataType, CellStyle> styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet(name);

            // Creating the header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(header.get(i));
                if (columnTypes != null && Set.of(TableDataType.DATE, TableDataType.DOUBLE, TableDataType.DATE_TIME).contains(columnTypes.get(i))) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Creating data rows
            for (int i = 0; i < table.size(); i++) {
                List<Object> row = table.get(i);
                Row excelRow = sheet.createRow(i + 1);

                for (int j = 0; j < row.size(); j++) {
                    Cell cell = excelRow.createCell(j);
                    Object value = row.get(j);
                    if (value != null) {
                        if (format) {
                            formatCell(cell, value, styles);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // Autosize
            if (columnTypes != null) {
                Set<TableDataType> autoSizeTypes = Set.of(TableDataType.DATE,
                        TableDataType.DOUBLE,
                        TableDataType.DATE_TIME,
                        TableDataType.STRING_AUTO_SIZE);
                for (int i = 0; i < columnTypes.size(); i++) {
                    if (autoSizeTypes.contains(columnTypes.get(i))) {
                        sheet.autoSizeColumn(i);
                    }
                }
            }
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void formatCell(Cell cell, @NonNull Object value, Map<TableDataType, CellStyle> styles) {
        if (value instanceof LocalDate) {
            cell.setCellValue((LocalDate) value);
            cell.setCellStyle(styles.get(TableDataType.DATE));
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue((LocalDateTime) value);
            cell.setCellStyle(styles.get(TableDataType.DATE_TIME));
        } else if (value instanceof Instant) {
            cell.setCellValue(LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault()));
            cell.setCellStyle(styles.get(TableDataType.DATE_TIME));
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
            cell.setCellStyle(styles.get(TableDataType.DOUBLE));
        } else {
            cell.setCellValue(objectMapperToString(value));
        }
    }

    private String objectMapperToString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String jsonString = objectMapper.writeValueAsString(value);
            return jsonString.startsWith("\"") ? jsonString.substring(1, jsonString.length() - 1) : jsonString;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<TableDataType, CellStyle> createStyles(Workbook workbook) {
        Map<TableDataType, CellStyle> styles = new HashMap<>();

        CellStyle dateTimeStyle = workbook.createCellStyle();
        DataFormat dateTimeFormat = workbook.createDataFormat();
        dateTimeStyle.setDataFormat(dateTimeFormat.getFormat("dd.MM.yyyy HH:mm:ss"));
        styles.put(TableDataType.DATE_TIME, dateTimeStyle);

        CellStyle dateStyle = workbook.createCellStyle();
        DataFormat dateFormat = workbook.createDataFormat();
        dateStyle.setDataFormat(dateFormat.getFormat("dd.MM.yyyy"));
        styles.put(TableDataType.DATE, dateStyle);

        CellStyle doubleStyle = workbook.createCellStyle();
        DataFormat doubleFormat = workbook.createDataFormat();
        doubleStyle.setDataFormat(doubleFormat.getFormat("#.###"));
        styles.put(TableDataType.DOUBLE, doubleStyle);

        return styles;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
