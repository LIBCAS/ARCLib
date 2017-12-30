package cz.inqool.uas.service;

import cz.inqool.uas.exception.MissingObject;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static cz.inqool.uas.util.Utils.asMap;
import static cz.inqool.uas.util.Utils.resource;
import static helper.ThrowableAssertion.assertThrown;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TemplaterTest {
    private Templater templater;

    @Before
    public void setUp() {
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.init();

        templater = new Templater();
        templater.setEngine(engine);
    }

    @Test
    public void noParamTest() throws IOException {

        String result = templater.transform(resource("cz/inqool/uas/service/noparam.vm"), emptyMap());

        assertThat(result, is("přidělen"));
    }

    @Test
    public void singleParamTest() throws IOException {
        String result = templater.transform(resource("cz/inqool/uas/service/single.vm"), asMap("param", "přidělen"));

        assertThat(result, is("přidělen"));
    }

    @Test
    public void doubleParamTest() throws IOException {
        String result = templater.transform(resource("cz/inqool/uas/service/double.vm"), asMap("param", "přidělen", "param2", "testing"));

        assertThat(result, is("přidělen\ntesting"));
    }

    @Test
    public void missingTemplateTest() throws IOException {
        assertThrown(() -> templater.transform(resource("cz/inqool/uas/service/nonexist.vm"), emptyMap()))
                .isInstanceOf(MissingObject.class);
    }
}
