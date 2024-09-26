package cz.cas.lib.arclib.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.util.ApplicationContextUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;

@Converter
public class AipQueryParamsDbConverter implements AttributeConverter<Params, String> {

    @Override
    public String convertToDatabaseColumn(Params value) {
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("could not convert object to JSON, object:" + value);
        }
    }


    @Override
    public Params convertToEntityAttribute(String value) {
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            return objectMapper.readValue(value, Params.class);
        } catch (IOException e) {
            throw new RuntimeException("could not convert JSON to object:" + value);
        }
    }

}