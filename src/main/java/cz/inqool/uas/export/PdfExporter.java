package cz.inqool.uas.export;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.service.Templater;
import cz.inqool.uas.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static cz.inqool.uas.util.Utils.resourceBytes;

/**
 * TODO: add support for images from classpath
 */

@Slf4j
@Service
public class PdfExporter {
    private Templater templater;

    @Value("${server.port}")
    private String serverPort;

    public byte[] export(InputStream template, Map<String, Object> arguments) {
        XRLog.setLoggerImpl(new Slf4jLogger());

        try {
            String resultHtml = templater.transform(template, arguments);

            byte[] colorProfile = Utils.resourceBytes("srgb_profile.icm");

            PdfBoxRenderer renderer = new PdfRendererBuilder()
                    .usePdfVersion(1.4f)
                    .usePdfAConformance("A")
                    .useColorProfile(colorProfile)
                    .defaultTextDirection(PdfRendererBuilder.TextDirection.LTR)
                    .withHtmlContent(resultHtml, "http://localhost:" + serverPort + "/")
                    .buildPdfRenderer();

            PdfBoxFontResolver fontResolver = renderer.getFontResolver();

            byte[] fontBytesArial = resourceBytes("arial.ttf");
            byte[] fontBytesTahoma = resourceBytes("tahomar.ttf");
            byte[] fontBytesTahomaBold = resourceBytes("tahomabd.ttf");
            byte[] fontBytesTahomaItalic = resourceBytes("verdanait.ttf");

            fontResolver.addFont(() -> new ByteArrayInputStream(fontBytesArial), "Ariel", null, null, false);
            fontResolver.addFont(() -> new ByteArrayInputStream(fontBytesTahoma), "Tahoma", null, null, false);
            fontResolver.addFont(() -> new ByteArrayInputStream(fontBytesTahomaBold), "Tahoma", 700, null, false);
            fontResolver.addFont(() -> new ByteArrayInputStream(fontBytesTahomaItalic), "Tahoma", null, IdentValue.ITALIC, false);

            renderer.layout();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            renderer.createPDF(out);

            return out.toByteArray();
        } catch (IOException ex) {
            throw new GeneralException(ex);
        }
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }
}
