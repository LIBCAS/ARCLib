package cz.inqool.uas.export;

import cz.inqool.uas.exception.GeneralException;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Service
public class XlsxExporter {
    public byte[] export(InputStream template, Map<String, Object> arguments) {
        try {
            Context context = new Context(arguments != null ? arguments : emptyMap());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            JxlsHelper.getInstance().processTemplate(template, out, context);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new GeneralException(ex);
        }
    }
}
