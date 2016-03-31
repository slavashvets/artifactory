package org.artifactory.storage.db.binstore.service;

import org.apache.commons.lang.StringUtils;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.storage.binstore.service.BinaryData;

/**
 * @author gidis
 */
public class BinaryDataWithValidation extends BinaryData {


    public BinaryDataWithValidation(String sha1, String md5, long length) {
        super(sha1, md5, length);
        isValid();
    }

    private void simpleValidation() {
        if (StringUtils.isBlank(getSha1()) || getSha1().length() != ChecksumType.sha1.length()) {
            throw new IllegalArgumentException("SHA1 value '" + getSha1() + "' is not a valid checksum");
        }
        if (StringUtils.isBlank(getMd5()) || getMd5().length() != ChecksumType.md5.length()) {
            throw new IllegalArgumentException("MD5 value '" + getMd5() + "' is not a valid checksum");
        }
        if (getLength() < 0L) {
            throw new IllegalArgumentException("Length " + getLength() + " is not a valid length");
        }
    }

    public boolean isValid() {
        simpleValidation();
        return ChecksumType.sha1.isValid(getSha1()) && ChecksumType.md5.isValid(getMd5());
    }
}
