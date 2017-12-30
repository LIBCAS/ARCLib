package cz.inqool.uas.export;

import cz.inqool.uas.exception.GeneralException;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class DocxExporter {
    public byte[] export(InputStream template, Map<String, Object> arguments) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IXDocReport report = XDocReportRegistry.getRegistry().loadReport(template, TemplateEngineKind.Velocity);
            IContext context = report.createContext(arguments);
            report.process(context, out);

            return out.toByteArray();
        } catch (XDocReportException | IOException ex) {
            throw new GeneralException(ex);
        }
    }
}
