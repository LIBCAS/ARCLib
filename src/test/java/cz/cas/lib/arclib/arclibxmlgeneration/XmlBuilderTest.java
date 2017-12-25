package cz.cas.lib.arclib.arclibxmlgeneration;

import cz.cas.lib.arclib.service.XmlBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XmlBuilderTest {

    private XmlBuilder xmlBuilder;
    private Map<String, String> uris;
    @Before
    public void setUp() {
        uris = new HashMap<>();
        uris.put("METS", "http://www.loc.gov/METS/");
        uris.put("ARCLIB", "http://arclib.lib.cas.cz/ARCLIB_XML");
        uris.put("PREMIS", "http://www.loc.gov/premis/v3");

        xmlBuilder = new XmlBuilder(uris);
    }

    @Test
    public void addChildNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child", "test value", uris.get("ARCLIB"));
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child xmlns=\"http://arclib.lib.cas.cz/ARCLIB_XML\">test value</child></root>"));
    }

    @Test
    public void addGrandChildNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child/grandchild", "test value", uris.get("ARCLIB"));
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child xmlns=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><grandchild>test value</grandchild></child></root>"));
    }

    @Test
    public void addSiblingNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        Element root = doc.getRootElement();

        Element child = DocumentHelper.createElement("child");
        child.setText("test value 1");

        root.add(child);

        xmlBuilder.addNode(doc, "/root/child", "test value 2", uris.get("ARCLIB"));
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>test value 1</child><child xmlns=\"http://arclib.lib.cas.cz/ARCLIB_XML\">test value 2</child></root>"));
    }

    @Test
    public void addAttributeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/@testAttribute", "test value", uris.get("ARCLIB"));
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root testAttribute=\"test value\"/>"));
    }

    @Test
    public void addEmptyValueTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child", null, uris.get("ARCLIB"));
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child xmlns=\"http://arclib.lib.cas.cz/ARCLIB_XML\"/></root>"));
    }
}
