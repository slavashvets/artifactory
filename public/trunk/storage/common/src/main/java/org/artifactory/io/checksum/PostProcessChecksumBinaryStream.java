package org.artifactory.io.checksum;

import org.artifactory.storage.binstore.binary.providers.base.BinaryStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Gidi Shabat
 */
public class PostProcessChecksumBinaryStream implements BinaryStream {
    private final Sha1Md5ChecksumInputStream checksumInputStream;

    public PostProcessChecksumBinaryStream(InputStream in) {
        this.checksumInputStream = new Sha1Md5ChecksumInputStream(in);
    }

    @Nonnull
    @Override
    public String getSha1() {
        return checksumInputStream.getSha1();
    }

    @Nonnull
    @Override
    public String getMd5() {
        return checksumInputStream.getMd5();
    }

    @Override
    public InputStream getStream() {
        return checksumInputStream;
    }

    @Override
    public long getLength() {
        return checksumInputStream.getTotalBytesRead();
    }

    @Override
    public void close() throws IOException {
        checksumInputStream.close();
    }
}
