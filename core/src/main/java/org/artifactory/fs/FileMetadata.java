package org.artifactory.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias("file")
public class FileMetadata extends FsItemMetadata {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(FileMetadata.class);

    private long lastUpdated;
    private long lastModified;
    private long downloadCount;
    private String mimeType;
    private boolean xmlAware;

    public FileMetadata() {
    }

    public FileMetadata(String repoKey, String relPath, long created, String modifiedBy,
            long lastUpdated, long lastModified, long downloadCount, String mimeType,
            boolean xmlAware) {
        super(repoKey, relPath, created, modifiedBy);
        this.lastUpdated = lastUpdated;
        this.lastModified = lastModified;
        this.downloadCount = downloadCount;
        this.mimeType = mimeType;
        this.xmlAware = xmlAware;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isXmlAware() {
        return xmlAware;
    }

    public void setXmlAware(boolean xmlAware) {
        this.xmlAware = xmlAware;
    }
}
