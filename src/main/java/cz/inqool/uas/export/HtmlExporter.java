package cz.inqool.uas.export;

import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.service.Templater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class HtmlExporter {
    private Templater templater;

    public byte[] export(InputStream template, Map<String, Object> arguments) {
        try {
            String resultHtml = templater.transform(template, arguments);
            return resultHtml.getBytes();
        } catch (Exception ex) {
            throw new GeneralException(ex);
        }
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }
}
