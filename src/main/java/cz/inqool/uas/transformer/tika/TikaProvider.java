package cz.inqool.uas.transformer.tika;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import static cz.inqool.uas.util.Utils.resource;

@Configuration
public class TikaProvider {
    @Bean
    public Tika tika() throws IOException, TikaException, SAXException {
        Tika tika;

        try (InputStream stream = resource("tika.xml")) {
            tika = new Tika(new TikaConfig(stream));
        }

        return tika;
    }
}
