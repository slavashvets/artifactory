/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.update.md.v130beta3;

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class ArtifactoryFileConverter implements MetadataConverter {
    public static final String ARTIFACTORY_FILE = "artifactory.file";

    public String getNewMetadataName() {
        return "artifactory-file";
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.file;
    }

    public void convert(Document doc) {
        Element rootElement = doc.getRootElement();
        rootElement.setName(getNewMetadataName());
        RepoPath repoPath = MetadataConverterUtils.extractRepoPath(rootElement);
        List<Element> toMove = MetadataConverterUtils.extractExtensionFields(rootElement);
        MetadataConverterUtils.addNewContent(rootElement, repoPath, toMove);
        ContentType ct = NamingUtils.getContentType(repoPath.getName());
        rootElement.removeChild("mimeType");
        rootElement.addContent(new Element("mimeType").setText(ct.getMimeType()));
    }

}
