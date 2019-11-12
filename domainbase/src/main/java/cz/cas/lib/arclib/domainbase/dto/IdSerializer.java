package cz.cas.lib.arclib.domainbase.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;

import java.io.IOException;

public class IdSerializer extends StdSerializer<DomainObject> {

    public IdSerializer() {
        super(DomainObject.class);
    }

    @Override
    public void serialize(DomainObject value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId());
        gen.writeEndObject();
    }
}
