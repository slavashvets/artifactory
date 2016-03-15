package org.artifactory.storage.binstore.binary.providers.base;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.InputStream;

/**
 * @author Gidi Shabat
 */
public interface BinaryStream extends Closeable {
    @Nonnull
    public String getSha1();

    @Nonnull
    public String getMd5();

    InputStream getStream();

    long getLength();

}
