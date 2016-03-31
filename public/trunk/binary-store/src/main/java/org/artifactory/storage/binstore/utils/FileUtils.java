package org.artifactory.storage.binstore.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by gidis on 22/02/2016.
 */
public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static boolean removeFile(File file) {
        if (file != null && file.exists()) {
            //Try to delete the file
            if (!remove(file)) {
                log.warn("Unable to remove " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private static boolean remove(final File file) {
        if (!file.delete()) {
            // NOTE: fix for java/win bug. see:
            // http://forum.java.sun.com/thread.jsp?forum=4&thread=158689&tstart=0&trange=15
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // nop
            }

            // Try one more time to delete the file
            return file.delete();
        }
        return true;
    }

    /**
     * Tries to determine why the file could not be read.
     * NOTICE: This method doesn't determine whether the file is accessible or not, just provides pretty reason string
     *
     * @param file to test
     * @return A string describing why the file cannot be read.
     */
    public static String readFailReason(File file) {
        Path asPath = file.toPath();

        if (!java.nio.file.Files.notExists(asPath)) {
            return "File not found";
        } else if (!java.nio.file.Files.isReadable(asPath)) {
            return "Access denied";
        } else {
            return "Unknown error";
        }

    }
}
