package org.artifactory.storage.binstore.binary.providers.base;

/**
 * @author Gidi Shabat
 */
public class BinaryInfoImpl implements BinaryInfo {
    private long length;
    private String md5;
    private String sha1;


    public BinaryInfoImpl(long length, String md5, String sha1) {
        this.length = length;
        this.md5 = md5;
        this.sha1 = sha1;
    }


    public BinaryInfoImpl(String sha1, String md5, long length) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.length = length;
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return sha1.equals(((BinaryInfoImpl) o).sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }

    @Override
    public String toString() {
        return "{" + sha1 + ',' + md5 + ',' + length + '}';
    }
}
