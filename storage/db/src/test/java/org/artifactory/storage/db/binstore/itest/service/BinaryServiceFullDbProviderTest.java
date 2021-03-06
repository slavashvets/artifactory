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

package org.artifactory.storage.db.binstore.itest.service;

import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryInfo;
import org.artifactory.util.ResourceUtils;
import org.jfrog.storage.binstore.exceptions.BinaryNotFoundException;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Date: 12/10/12
 * Time: 9:54 PM
 *
 * @author freds
 */
@Test
public class BinaryServiceFullDbProviderTest extends BinaryServiceBaseTest {

    @Override
    protected StorageProperties.BinaryProviderType getBinaryStoreType() {
        return StorageProperties.BinaryProviderType.fullDb;
    }

    @Override
    protected String getBinaryStoreDirName() {
        return "cache_test";
    }

    @Override
    protected void assertBinaryExistsEmpty(String sha1) {
        try {
            binaryStore.getBinary(sha1);
            fail("Should have send " + BinaryNotFoundException.class + " exception!");
        } catch (BinaryNotFoundException e) {
            // Message should be "Couldn't find content for '" + sha1 + "'"
            String message = e.getMessage();
            assertTrue(message.contains("content for '" + sha1 + "'"), "Wrong exception message " + message);
        }
    }

    @Override
    protected BinaryInfo addBinary(String resName, String sha1, String md5, long length) throws IOException {
        return binaryStore.addBinary(ResourceUtils.getResource("/binstore/" + resName));
    }

    // TODO [By Gidi] check this change with Fred or Yossi
    @Override
    protected void testPruneAfterLoad() {
        Object[][] binFileData = getBinFileData();
        int totSize = 0;
//        for (Object[] objects : binFileData) {
//            totSize += ((Long) objects[3]).intValue();
//        }
        testPrune(0, 0, totSize);
    }

    @Override
    protected void assertPruneAfterOneGc() {
        testPrune(0, 0, 0);
    }
}
