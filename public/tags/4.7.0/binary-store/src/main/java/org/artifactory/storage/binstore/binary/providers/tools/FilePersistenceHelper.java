package org.artifactory.storage.binstore.binary.providers.tools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.storage.binstore.binary.providers.base.BinaryInfo;
import org.artifactory.storage.binstore.binary.providers.base.BinaryInfoImpl;
import org.artifactory.storage.binstore.binary.providers.base.BinaryStream;
import org.artifactory.storage.binstore.binary.providers.base.FileProviderStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;

/**
 * @author Gidi Shabat
 */
public class FilePersistenceHelper {
    private static final Logger log = LoggerFactory.getLogger(FilePersistenceHelper.class);
    private static Random random;

    private static synchronized Random initRNG() {
        Random rnd = random;
        return (rnd == null) ? (random = new Random()) : rnd;
    }

    public static BinaryInfoImpl saveStreamToTempFile(BinaryStream in, File preFileStoreFile) throws IOException {
        try {
            log.trace("Saving temp file:  '{}'", preFileStoreFile.getAbsolutePath());
            FileUtils.copyInputStreamToFile(in.getStream(), preFileStoreFile);
            log.trace("Saved  temp file:  '{}'", preFileStoreFile.getAbsolutePath());
            long fileLength = preFileStoreFile.length();
            if (fileLength != in.getLength()) {
                throw new IOException("File length is " + fileLength + " while total bytes read on stream is " +
                        in.getLength());
            }
            return new BinaryInfoImpl(preFileStoreFile.length(), in.getMd5(), in.getSha1());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static File persistFile(BinaryInfo binaryInfo, File preFileStoreFile,
                                   FileProviderStrategy fileProviderStrategy) throws IOException {
        // move the file to its final destination
        File targetFile = fileProviderStrategy.getFile(binaryInfo.getSha1());
        Path targetPath = targetFile.toPath();
        if (!fileProviderStrategy.isFileExists(binaryInfo.getSha1())) {
            // Move the file from the pre-filestore to the filestore
            java.nio.file.Files.createDirectories(targetPath.getParent());
            try {
                log.trace("Moving {} to  '{}'", preFileStoreFile.getAbsolutePath(), targetPath);
                java.nio.file.Files.move(preFileStoreFile.toPath(), targetPath, StandardCopyOption.ATOMIC_MOVE);
                log.trace("Moved  {} to  '{}'", preFileStoreFile.getAbsolutePath(), targetPath);
            } catch (FileAlreadyExistsException ignore) {
                // May happen in heavy concurrency cases
                log.trace("Failed moving  '{}'  to  '{}'. File already exist", preFileStoreFile.getAbsolutePath(),
                        targetPath);
            } catch (AccessDeniedException exception) {
                log.trace("Failed moving  '{}'  to  '{}'. Access to file denied", preFileStoreFile.getAbsolutePath(),
                        targetPath);
            }
            return null;
        } else {
            log.trace("File  '{}'  already exist in the file store. Deleting temp file:  '{}'",
                    targetPath, preFileStoreFile.getAbsolutePath());
            return preFileStoreFile;
        }
    }

    public static BinaryInfo saveStreamFileAndMove(BinaryStream in, FileProviderStrategy fileProviderStrategy)
            throws IOException {
        File preFileStoreFile = null;
        try {
            preFileStoreFile = fileProviderStrategy.createTempFile();
            BinaryInfoImpl binaryInfo = saveStreamToTempFile(in, preFileStoreFile);
            preFileStoreFile = persistFile(binaryInfo, preFileStoreFile, fileProviderStrategy);
            return binaryInfo;
        } finally {
            if (preFileStoreFile != null && preFileStoreFile.exists()) {
                if (!preFileStoreFile.delete()) {
                    log.error("Could not delete temp file  '{}'", preFileStoreFile.getAbsolutePath());
                }
            }
        }
    }

    public static File createTempBinFile(File tempDir) {
        Random rnd = random;
        if (rnd == null) {
            rnd = initRNG();
        }
        long n = rnd.nextLong();
        if (n == Long.MIN_VALUE) {
            n = 0;      // corner case
        } else {
            n = Math.abs(n);
        }
        return new File(tempDir, "dbRecord" + n + ".bin");
    }

    public static String getRelativePath(String sha1) {
        return sha1.substring(0, 2) + "/" + sha1;
    }
}
