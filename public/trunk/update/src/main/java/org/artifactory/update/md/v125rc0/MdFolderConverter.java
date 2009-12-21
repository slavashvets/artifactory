/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.update.md.v125rc0;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * @author freds
 * @date Nov 11, 2008
 */
public class MdFolderConverter implements MetadataConverter {
    static final String ARTIFACTORY_NAME = "artifactoryName";

    public String getNewMetadataName() {
        return "artifactory-folder";
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.folder;
    }

    public void convert(Document doc) {
        Element rootElement = doc.getRootElement();
        rootElement.setName(getNewMetadataName());
        RepoPath repoPath = MetadataConverterUtils.extractRepoPath(rootElement);
        // In this version the relPath is the father and name need to be added
        if (rootElement.getChild(ARTIFACTORY_NAME) != null) {
            String name = rootElement.getChildText(ARTIFACTORY_NAME);
            repoPath = new RepoPath(repoPath, name);
        }
        List<Element> toMove = MetadataConverterUtils.extractExtensionFields(rootElement);
        MetadataConverterUtils.addNewContent(rootElement, repoPath, toMove);
        // Not used anymore
        rootElement.removeChild(ARTIFACTORY_NAME);
    }
}
