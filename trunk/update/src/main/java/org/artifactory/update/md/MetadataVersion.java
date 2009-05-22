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
package org.artifactory.update.md;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.update.md.current.MetadataReaderImpl;
import org.artifactory.update.md.v125rc0.MdFileConverter;
import org.artifactory.update.md.v125rc0.MdFolderConverter;
import org.artifactory.update.md.v125rc0.MdStatsConverter;
import org.artifactory.update.md.v125rc0.MetadataReader125;
import org.artifactory.update.md.v130beta3.ArtifactoryFileConverter;
import org.artifactory.update.md.v130beta3.ArtifactoryFolderConverter;
import org.artifactory.update.md.v130beta3.MetadataReader130beta3;
import org.artifactory.update.md.v130beta6.ChecksumsConverter;
import org.artifactory.update.md.v130beta6.FolderAdditionalInfoNameConverter;
import org.artifactory.update.md.v130beta6.MetadataReader130beta6;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.VersionComparator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Nov 11, 2008
 */
public enum MetadataVersion implements MetadataReader {
    notSupported(ArtifactoryVersion.v122rc0, ArtifactoryVersion.v122, null),
    v125rc0(ArtifactoryVersion.v125rc0, ArtifactoryVersion.v130beta2, new MetadataReader125(),
            new MdFolderConverter(), new MdFileConverter(), new MdStatsConverter()),
    v130beta3(ArtifactoryVersion.v130beta3, ArtifactoryVersion.v130beta5, new MetadataReader130beta3(),
            new ArtifactoryFolderConverter(), new ArtifactoryFileConverter()),
    v130beta6(ArtifactoryVersion.v130beta6, ArtifactoryVersion.v130beta61, new MetadataReader130beta6(),
            new FolderAdditionalInfoNameConverter(), new ChecksumsConverter()),
    current(ArtifactoryVersion.v130rc1, ArtifactoryVersion.getCurrent(), new MetadataReaderImpl());

    private final static Logger log = LoggerFactory.getLogger(MetadataVersion.class);

    private static final String FILE_MD_NAME_V130_BETA_3 = ArtifactoryFileConverter.ARTIFACTORY_FILE + ".xml";
    private static final String FOLDER_MD_NAME_V130_BETA_3 = ArtifactoryFolderConverter.ARTIFACTORY_FOLDER + ".xml";
    private static final String FILE_MD_NAME_V130_BETA_6 = ChecksumsConverter.ARTIFACTORY_FILE + ".xml";
    private static final String FOLDER_MD_NAME_V130_BETA_6 =
            FolderAdditionalInfoNameConverter.ARTIFACTORY_FOLDER + ".xml";

    private final VersionComparator comparator;
    private final MetadataReader delegate;
    private final MetadataConverter[] converters;

    /**
     * @param from       The artifactory version this metadata format was first used in
     * @param until      The latest artifactory version this metadata format was valid
     * @param converters A list of converters to use to convert the metadata from this version range to the next
     */
    MetadataVersion(ArtifactoryVersion from, ArtifactoryVersion until, MetadataReader delegate,
            MetadataConverter... converters) {
        this.comparator = new VersionComparator(from, until);
        this.delegate = delegate;
        this.converters = converters;
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public boolean supports(ArtifactoryVersion version) {
        return comparator.supports(version);
    }

    public VersionComparator getComparator() {
        return comparator;
    }

    public List<MetadataEntry> getMetadataEntries(File file, ImportSettings settings, StatusHolder status) {
        if (delegate == null) {
            throw new IllegalStateException("Metadata Import from version older than 1.2.2 is not supported!");
        }
        return delegate.getMetadataEntries(file, settings, status);
    }

    /**
     * Find the version from the format of the metadata folder
     *
     * @param metadataFolder
     */
    public static MetadataVersion findVersion(File metadataFolder) {
        if (!metadataFolder.exists()) {
            throw new IllegalArgumentException(
                    "Cannot find metadata version of non existent file " + metadataFolder.getAbsolutePath());
        }
        if (metadataFolder.isDirectory()) {
            File[] mdFiles = metadataFolder.listFiles();
            for (File mdFile : mdFiles) {
                String mdFileName = mdFile.getName();
                if (mdFileName.equalsIgnoreCase(FILE_MD_NAME_V130_BETA_3) ||
                        mdFileName.equalsIgnoreCase(FOLDER_MD_NAME_V130_BETA_3)) {
                    return v130beta3;
                }
                if (mdFileName.equalsIgnoreCase(FILE_MD_NAME_V130_BETA_6) ||
                        mdFileName.equalsIgnoreCase(FOLDER_MD_NAME_V130_BETA_6)) {
                    // here we don't know if it's beta6 or rc1+ so we must read the xml and decide by the content
                    // must improve this in the future.
                    Document doc;
                    try {
                        doc = buildDocFromFile(mdFile);
                    } catch (Exception e) {
                        // log and try the next file
                        log.warn("Failed to read file '" + mdFile + "' as xml", e);
                        continue;
                    }
                    Element root = doc.getRootElement();
                    Element extension = root.getChild("extension");
                    if (extension != null) {
                        return v130beta6;
                    } else {
                        return current;
                    }
                }
            }
            throw new IllegalStateException("Metadata folder " + metadataFolder.getAbsolutePath() +
                    " does not contain any recognizable metadata files!");
        } else {
            // For v125rc0 to v130beta2, the folder is actually a file
            return v125rc0;
        }
    }

    private static Document buildDocFromFile(File file) throws Exception {
        SAXBuilder sb = new SAXBuilder();
        InputStream in = null;
        try {
            return sb.build(new FileInputStream(file));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static MetadataVersion findVersion(ArtifactoryVersion version) {
        MetadataVersion result = null;
        MetadataVersion[] metadataVersions = values();
        for (int i = metadataVersions.length - 1; i >= 0; i--) {
            MetadataVersion metadataVersion = metadataVersions[i];
            if (metadataVersion.supports(version)) {
                result = metadataVersion;
                break;
            }
        }
        if (result == null || result == notSupported) {
            throw new IllegalStateException("Metadata import from Artifactory version " + version +
                    " is not supported!");
        }
        return result;
    }

    public static List<MetadataConverter> getConvertersFor(MetadataVersion mdVersion, MetadataType type) {
        int scanFrom = mdVersion.ordinal();
        List<MetadataConverter> converters = new ArrayList<MetadataConverter>();
        MetadataVersion[] versions = MetadataVersion.values();
        for (int i = scanFrom; i < versions.length; i++) {
            MetadataConverter[] mdConverters = versions[i].converters;
            for (MetadataConverter converter : mdConverters) {
                if (type.equals(converter.getSupportedMetadataType())) {
                    converters.add(converter);
                }
            }
        }
        return converters;
    }

}
