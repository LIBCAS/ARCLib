package cz.inqool.uas.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Transformer {
    boolean support(String inType, String outType);

    void transform(String inType, String outType, InputStream in, OutputStream out) throws IOException;
}
