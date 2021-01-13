package cz.cas.lib.arclib.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.notification.NotificationElement;
import cz.cas.lib.core.util.ApplicationContextUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

@Converter
public class NotificationComponentConverter implements AttributeConverter<List<NotificationElement>, String> {

    @Override
    public String convertToDatabaseColumn(List<NotificationElement> attribute) {
        if (attribute == null)
            return null;
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert object to JSON, object:" + attribute);
        }
    }

    @Override
    public List<NotificationElement> convertToEntityAttribute(String dbData) {
        if (dbData == null)
            return null;
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            return Arrays.asList(objectMapper.readValue(dbData, NotificationElement[].class));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to parse JSON from DB", e);
        }
    }

}