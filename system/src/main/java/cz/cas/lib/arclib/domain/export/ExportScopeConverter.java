package cz.cas.lib.arclib.domain.export;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;

@Converter
public class ExportScopeConverter implements AttributeConverter<Collection<ExportScope>, String> {
    static final ObjectMapper mapper = new ObjectMapper();
    private static final CollectionType JSON_TYPE_REFERENCE = TypeFactory.defaultInstance().constructCollectionType(Collection.class, ExportScope.class);


    @Override
    public String convertToDatabaseColumn(Collection<ExportScope> c) {
        if (c == null)
            return null;
        try {
            return mapper.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Collection<ExportScope> convertToEntityAttribute(String dbJson) {
        if (dbJson == null)
            return null;
        try {
            return mapper.readValue(dbJson, JSON_TYPE_REFERENCE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
