package cz.inqool.uas.transformer;

import cz.inqool.uas.exception.GeneralException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;

@Service
public class TikaTransformer implements Transformer {
    private Tika tika;

    @Override
    public boolean support(String inType, String outType) {
        Set<MediaType> supportedTypes = tika.getParser().getSupportedTypes(new ParseContext());

        return supportedTypes.contains(MediaType.parse(inType)) && outType.equals("text/plain");
    }

    @Override
    public void transform(String inType, String outType, InputStream in, OutputStream out) throws IOException {
        if (support(inType, outType)) {

            try {
                String text = tika.parseToString(in).trim();

                out.write(text.getBytes(Charset.forName("UTF-8")));

            } catch (TikaException e) {
                throw new GeneralException(e);
            }
        }
    }

    @Inject
    public void setTika(Tika tika) {
        this.tika = tika;
    }
}
