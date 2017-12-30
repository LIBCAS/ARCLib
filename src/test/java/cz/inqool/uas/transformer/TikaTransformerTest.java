package cz.inqool.uas.transformer;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import cz.inqool.uas.transformer.tika.TikaProvider;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static cz.inqool.uas.util.Utils.resource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TikaTransformerTest {
    private TikaTransformer transformer;

    @Before
    public void setUp() throws TikaException, IOException, SAXException {
        TikaProvider provider = new TikaProvider();
        Tika tika = provider.tika();

        transformer = new TikaTransformer();
        transformer.setTika(tika);
    }

    @Test
    public void pdfTest() throws IOException {
        ByteArrayOutputStream in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream stream = resource("cz/inqool/uas/transformer/test.pdf")) {
            in = new ByteArrayOutputStream();

            ByteStreams.copy(stream, in);
        }

        boolean support = transformer.support("application/pdf", "text/plain");
        assertThat(support, is(true));

        transformer.transform("application/pdf", "text/plain", new ByteArrayInputStream(in.toByteArray()), out);
        assertThat(new String(out.toByteArray(), "UTF-8"), is("Test\tPDF"));
    }

    @Test
    public void docxTest() throws IOException {
        ByteArrayOutputStream in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream stream = resource("cz/inqool/uas/transformer/test.docx")) {
            in = new ByteArrayOutputStream();

            ByteStreams.copy(stream, in);
        }

        boolean support = transformer.support("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain");
        assertThat(support, is(true));

        transformer.transform("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", new ByteArrayInputStream(in.toByteArray()), out);
        assertThat(new String(out.toByteArray(), "UTF-8"), is("Test Docx"));
    }

    @Test
    public void docTest() throws IOException {
        ByteArrayOutputStream in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream stream = resource("cz/inqool/uas/transformer/test.doc")) {
            in = new ByteArrayOutputStream();

            ByteStreams.copy(stream, in);
        }

        boolean support = transformer.support("application/msword", "text/plain");
        assertThat(support, is(true));

        transformer.transform("application/msword", "text/plain", new ByteArrayInputStream(in.toByteArray()), out);
        assertThat(new String(out.toByteArray(), "UTF-8"), is("Test Doc"));
    }

    @Test
    public void htmlTest() throws IOException {
        ByteArrayOutputStream in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream stream = resource("cz/inqool/uas/transformer/test.html")) {
            in = new ByteArrayOutputStream();

            ByteStreams.copy(stream, in);
        }

        boolean support = transformer.support("text/html", "text/plain");
        assertThat(support, is(true));

        transformer.transform("text/html", "text/plain", new ByteArrayInputStream(in.toByteArray()), out);
        assertThat(new String(out.toByteArray(), "UTF-8"), is("přidělen\n\nnení nadpis"));
    }


    @Test
    public void unsupportedTest() throws IOException {
        ByteArrayOutputStream in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream stream = resource("cz/inqool/uas/transformer/test.txt")) {
            in = new ByteArrayOutputStream();

            ByteStreams.copy(stream, in);
        }

        boolean support = transformer.support("fake/fake", "text/plain");
        assertThat(support, is(false));

        transformer.transform("fake/fake", "text/plain", new ByteArrayInputStream(in.toByteArray()), out);
        assertThat(out.toByteArray(), is(new byte[0]));
    }
}
