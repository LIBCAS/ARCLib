package cz.inqool.uas.export.pdf;

import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.pdfboxout.PdfBoxUserAgent;
import com.openhtmltopdf.swing.NaiveUserAgent;

public class SpringUserAgentCallback extends PdfBoxUserAgent {

    public SpringUserAgentCallback(PdfBoxOutputDevice outputDevice) {
        super(outputDevice);
    }
}
