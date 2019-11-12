package cz.cas.lib.core.index.solr.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TemporalConverters {

    public static String localDateToIsoUtcString(LocalDate source) {
        if (source == null)
            return null;
        return source.format(DateTimeFormatter.ISO_DATE);
    }

    public static String instantToIsoUtcString(Instant source) {
        if (source == null)
            return null;
        return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC).format(source);
    }

    public static Date isoStringToDate(String source) {
        if (source == null)
            return null;
        return Date.from(Instant.parse(source));
    }
}
