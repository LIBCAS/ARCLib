package cz.inqool.uas.admin;

import cz.inqool.uas.script.ScriptExecutor;
import cz.inqool.uas.script.ScriptType;
import cz.inqool.uas.util.Utils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ScriptExecutorTest {
    private ScriptExecutor executor;

    @Before
    public void setUp() {
        executor = new ScriptExecutor();
    }

    @Test
    public void testSimpleJavascript() {
        String result = executor.executeScript(ScriptType.JAVASCRIPT, "4 + 5").toString();
        assertThat(result, is("9"));
    }

    @Test
    public void testSimpleGroovy() {
        String result = executor.executeScript(ScriptType.GROOVY, "3 + 2").toString();
        assertThat(result, is("5"));
    }

    @Test
    public void testComplexGroovy() throws IOException {
        String script = Utils.resourceString("cz/inqool/uas/admin/script.groovy");
        String result = executor.executeScript(ScriptType.GROOVY, script).toString();

        String compareText = "test test2 " + LocalDate.now();
        assertThat(result, is(compareText));
    }

    @Test
    public void testComplexJavascript() throws IOException {
        String script = Utils.resourceString("cz/inqool/uas/admin/script.js");
        String result = executor.executeScript(ScriptType.JAVASCRIPT, script).toString();

        String compareText = "test test2 " + LocalDate.now();
        assertThat(result, is(compareText));
    }
}
