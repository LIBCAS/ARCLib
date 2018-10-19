package cz.cas.lib.core.service;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Wrapper class around Velocity engine templater.
 */
@Service
public class Templater {
    private VelocityEngine engine;

    public String transform(InputStream template, Map<String, Object> arguments) {
        notNull(template, () -> new IllegalArgumentException("template"));

        BufferedReader reader = new BufferedReader(new InputStreamReader(template, StandardCharsets.UTF_8));
        return transform(reader, arguments);
    }

    public String transform(String input, Map<String, Object> arguments) {
        notNull(input, () -> new IllegalArgumentException("input"));

        return transform(new StringReader(input), arguments);
    }

    protected String transform(Reader reader, Map<String, Object> arguments) {
        notNull(reader, () -> new IllegalArgumentException("reader"));

        StringWriter result = new StringWriter();
        VelocityContext velocityContext = new VelocityContext(arguments);

        engine.evaluate(velocityContext, result, "", reader);

        return result.toString();
    }

    @Inject
    public void setEngine(VelocityEngine engine) {
        this.engine = engine;
    }
}
