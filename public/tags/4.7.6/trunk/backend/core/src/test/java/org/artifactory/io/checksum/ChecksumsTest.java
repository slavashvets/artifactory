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

package org.artifactory.io.checksum;

import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.util.Pair;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests the ChecksumCalculator.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumsTest {

    public void calculateSha1() throws IOException {
        byte[] bytes = "this is a test".getBytes();
        String result = ChecksumUtil.getChecksum(new ByteArrayInputStream(bytes), ChecksumType.sha1);
        assertEquals(result, "fa26be19de6bff93f70bc2308434e4a440bbad02",
                "Wrong SHA1 calculated");
    }

    public void calculateMd5() throws IOException {
        byte[] bytes = "this is a test".getBytes();
        String result = ChecksumUtil.getChecksum(new ByteArrayInputStream(bytes), ChecksumType.md5);
        assertEquals(result, "54b0c58c7ce9f2a8b551351102ee0938",
                "Wrong SHA1 calculated");
    }

    public void calculateSha1AndMd5() throws IOException {
        byte[] bytes = "and this is another test".getBytes();
        ChecksumsInfo results = ChecksumUtil.getChecksumsInfo(new ByteArrayInputStream(bytes));
        assertNotNull(results, "Results should not be null");
        assertEquals(results.size(), 2, "Expecting two calculated value");
        assertEquals(results.getSha1(), "5258d99970d60aed055c0056a467a0422acf7cb8",
                "Wrong SHA1 calculated");
        assertEquals(results.getMd5(), "72f1aea68f75f79889b99cd4ff7acc83",
                "Wrong MD5 calculated");
    }

    public void calculateAllKnownChecksums() throws IOException {
        byte[] bytes = "and this is yet another test".getBytes();
        Pair<Long, ChecksumsInfo> results = ChecksumUtil.getSizeAndChecksumsInfo(new ByteArrayInputStream(bytes), ChecksumType.values());
        ChecksumsInfo checksumsInfo = results.getSecond();
        assertNotNull(checksumsInfo, "Results should not be null");
        assertEquals(checksumsInfo.size(), ChecksumType.values().length, "Expecting all checksums calculated value");
        assertEquals(checksumsInfo.getSha1(), "4e125432334dc76048aab3132a1bbc03c79f27e9",
                "Wrong SHA1 calculated");
        assertEquals(checksumsInfo.getMd5(), "0df8861dcef78d35aae9c6c6c8c69506",
                "Wrong MD5 calculated");
        assertEquals(checksumsInfo.getSha256(), "02a5357cde4bf21ba850fce37721406475a0c311dbbdffc2c79148ece46049b3",
                "Wrong SHA256 calculated");
    }

}
