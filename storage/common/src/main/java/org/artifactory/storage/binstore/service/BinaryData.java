/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.storage.binstore.service;

import java.io.Serializable;

/**
 * Represents a binary data entry in the database.
 *
 * @author Yossi Shaul
 */
public class BinaryData implements Serializable {
    private final String sha1;
    private final String md5;
    private final long length;

    public BinaryData(String sha1, String md5, long length) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.length = length;
    }

    public String getSha1() {
        return sha1;
    }

    public String getMd5() {
        return md5;
    }

    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "{" + sha1 + ',' + md5 + ',' + length + '}';
    }

}
