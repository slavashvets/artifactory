package org.artifactory.version.converter.v172;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * @author Dan Feldman
 */
public class BlockMismatchingMimeTypesConverterTest extends XmlConverterTest {

    @Test
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config-1.6.8-expires_in.xml", new BlockMismatchingMimeTypesConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        rootElement.getChild("remoteRepositories", namespace).getChildren().stream()
                .forEach(remoteRepo -> {
                    String blockMime = remoteRepo.getChild("blockMismatchingMimeTypes", namespace).getText();
                    assertTrue(blockMime.equals("true"));
                });
    }
}
