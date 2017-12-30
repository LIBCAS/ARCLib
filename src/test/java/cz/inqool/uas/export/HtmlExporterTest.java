package cz.inqool.uas.export;

import cz.inqool.uas.service.Templater;
import cz.inqool.uas.transformer.TikaTransformer;
import cz.inqool.uas.transformer.tika.TikaProvider;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static cz.inqool.uas.util.Utils.resource;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class HtmlExporterTest {
    private HtmlExporter exporter;

    private TikaTransformer transformer;

    @Before
    public void setUp() throws TikaException, IOException, SAXException {
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.init();

        Templater templater = new Templater();
        templater.setEngine(engine);

        exporter = new HtmlExporter();
        exporter.setTemplater(templater);

        TikaProvider provider = new TikaProvider();
        Tika tika = provider.tika();

        transformer = new TikaTransformer();
        transformer.setTika(tika);
    }

    @Test
    public void simpleExport() throws IOException {
        byte[] html = exporter.export(resource("cz/inqool/uas/export/htmlTest.vm"), emptyMap());

        String result = new String(html, "UTF-8");

        result = result.replaceAll("\r", "");

        assertThat(result, is("<!DOCTYPE html>\n<html>\n    <body style=\"font-family: Ariel\">\n" +
                "        test\n    </body>\n</html>"));
    }
}
