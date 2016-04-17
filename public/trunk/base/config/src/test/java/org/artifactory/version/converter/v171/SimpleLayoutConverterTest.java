package org.artifactory.version.converter.v171;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Shay Yaakov
 */
public class SimpleLayoutConverterTest extends XmlConverterTest {

    @Test
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config-1.6.8-expires_in.xml", new SimpleLayoutConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        List<Element> repoLayouts = rootElement.getChild("repoLayouts", namespace).getChildren();
        boolean foundSimpleLayout = false;
        for (Element repoLayout : repoLayouts) {
            String layoutName = repoLayout.getChild("name", namespace).getText();
            if (layoutName.equals("simple-default")) {
                foundSimpleLayout = true;
                String pattern = repoLayout.getChild("artifactPathPattern", namespace).getText();
                assertEquals(pattern, "[orgPath]/[module]/[module]-[baseRev].[ext]");
                break;
            }
        }
        assertTrue(foundSimpleLayout);
    }
}