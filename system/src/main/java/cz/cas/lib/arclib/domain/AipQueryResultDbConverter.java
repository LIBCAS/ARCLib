package cz.cas.lib.arclib.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.util.ApplicationContextUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
public class AipQueryResultDbConverter implements AttributeConverter<Result<IndexedArclibXmlDocument>, String> {

    @Override
    public String convertToDatabaseColumn(Result<IndexedArclibXmlDocument> value) {
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("could not convert object to JSON, object:" + value);
        }
    }


    @Override
    public Result<IndexedArclibXmlDocument> convertToEntityAttribute(String value) {
        try {
            ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
            JavaType type = objectMapper.getTypeFactory().constructParametricType(Result.class, IndexedArclibXmlDocument.class);
            return objectMapper.readValue(value, type);
        } catch (IOException e) {
            throw new RuntimeException("could not convert JSON to object:" + value);
        }
    }

}