package cz.cas.lib.arclib.formatlibrary.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;

import java.io.IOException;

public class FormatDefinitionIdDeserializer extends StdDeserializer<FormatDefinition> {

    protected FormatDefinitionIdDeserializer() {
        super(FormatDefinition.class);
    }

    @Override
    public FormatDefinition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        FormatDefinition formatDefinition = new FormatDefinition();
        String id = p.getValueAsString("id");
        formatDefinition.setId(id);
        return formatDefinition;
    }
}
