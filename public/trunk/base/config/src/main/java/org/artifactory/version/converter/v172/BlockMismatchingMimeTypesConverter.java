package org.artifactory.version.converter.v172;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the 'Block Mismatching Mime Types' flag (on) as default for all remote repositories
 *
 * @author Dan Feldman
 */
public class BlockMismatchingMimeTypesConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(BlockMismatchingMimeTypesConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting conversion: add 'Block mismatching mime type' flag, on by default, to all remote repos ");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element remoteRepos = rootElement.getChild("remoteRepositories", namespace);
        if (remoteRepos != null && !remoteRepos.getChildren().isEmpty()) {
            remoteRepos.getChildren().forEach(this::addDefaultBlockMimeTypes);
        }
        log.info("Finished mismatching mime types conversion.");
    }

    private void addDefaultBlockMimeTypes(Element repo) {
        Element blockMime = repo.getChild("blockMismatchingMimeTypes", repo.getNamespace());
        if (blockMime == null) {
            blockMime = new Element("blockMismatchingMimeTypes", repo.getNamespace());
            int lastLocation = findLocationToInsert(repo);
            repo.addContent(lastLocation + 1, new Text("\n            "));
            repo.addContent(lastLocation + 2, blockMime);
            blockMime.setText("true");
        }
    }

    private int findLocationToInsert(Element repo) {
        return findLastLocation(repo, "contentSynchronisation",
                "vcs",
                "p2OriginalUrl",
                "cocoaPods",
                "bower",
                "pypi",
                "nuget",
                "p2Support",
                "rejectInvalidJars",
                "remoteRepoLayoutRef",
                "listRemoteFolderItems",
                "synchronizeProperties",
                "shareConfiguration",
                "unusedArtifactsCleanupPeriodHours",
                "remoteRepoChecksumPolicyType",
                "missedRetrievalCachePeriodSecs",
                "assumedOfflinePeriodSecs",
                "retrievalCachePeriodSecs",
                "fetchSourcesEagerly",
                "fetchJarsEagerly",
                "storeArtifactsLocally",
                "hardFail",
                "offline",
                "url");
    }

    private int findLastLocation(Element parent, String... elements) {
        for (String element : elements) {
            Element child = parent.getChild(element, parent.getNamespace());
            if (child != null) {
                return parent.indexOf(child);
            }
        }
        return -1;
    }
}
