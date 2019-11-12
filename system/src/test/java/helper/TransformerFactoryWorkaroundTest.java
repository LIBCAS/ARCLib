package helper;

import org.junit.BeforeClass;

/**
 * has to be used in springboot tests
 */
public abstract class TransformerFactoryWorkaroundTest {
    @BeforeClass
    public static void workaroundIt() {
        System.setProperty("javax.xml.transform.TransformerFactory", "cz.cas.lib.arclib.config.ArclibTransformerFactory");
    }
}
