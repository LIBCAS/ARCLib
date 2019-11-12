package cz.cas.lib.arclib.service.preservationPlanning;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.domain.RelatedFormat;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DtoTests {

    @Test
    public void relatedFormatSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        FormatDefinition fd = new FormatDefinition();
        fd.setPreferred(false);
        fd.setFormatNote("bllaaaaa");
        RelatedFormat rf = new RelatedFormat();
        rf.setFormatDefinition(fd);
        rf.setRelatedFormatId(150);
        String s = objectMapper.writeValueAsString(rf);
        assertThat(s, containsString(fd.getId()));
        assertThat(s, not(containsString(fd.getFormatNote())));
        String replaced = s.replace("\"formatDefinition\":{", "\"formatDefinition\":{\"formatNote\":\"blaaaa\", ");
        RelatedFormat relatedFormat = objectMapper.readValue(replaced, RelatedFormat.class);
        assertThat(relatedFormat.getFormatDefinition().getFormatNote(), nullValue());
    }
}
