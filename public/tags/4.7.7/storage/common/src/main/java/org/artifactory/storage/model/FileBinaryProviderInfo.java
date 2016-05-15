package org.artifactory.storage.model;

import java.io.File;

/**
 * @author gidis
 */
public class FileBinaryProviderInfo {
    private File tempDir;
    private File fileStoreDir;
    private String type;

    public FileBinaryProviderInfo(File tempDir, File fileStoreDir, String type) {
        this.tempDir = tempDir;
        this.fileStoreDir = fileStoreDir;
        this.type = type;
    }

    public File getTempDir() {
        return tempDir;
    }

    public File getFileStoreDir() {
        return fileStoreDir;
    }

    public String getType() {
        return type;
    }
}
