package org.artifactory.io;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Chen Keinan
 */
public class FileUtils {
    private static final int TEMP_DIR_CREATE_ATTEMPTS = 10;
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    private static final long MEGABYTE = 1024L * 1024L;
    public static long bytesToMeg(long bytes) {
        return bytes / MEGABYTE;
    }

    /**
     * write byte array to file
     *
     * @param filename file name
     * @param content  byte array
     */
    public static void writeFile(String filename, byte[] content) {
        File file = new File(filename);
        FileOutputStream fop = null;
        try {
            /// check if file exist
            if (!file.exists()) {
                if (file.createNewFile()) {
                    fop = new FileOutputStream(file);
                    fop.write(content);
                    fop.flush();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot write file to temporary directory");
        } finally {
            IOUtil.close(fop);
        }
    }


    /**
     * copy input stream to file
     *
     * @param in   - input stream
     * @param file - files
     */
    public static void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot copy file input stream");
        } finally {
            IOUtil.close(out);
            IOUtil.close(in);
        }
    }

    public static long bytesToMB(long sizeInBytes) {
        return sizeInBytes / (1024 * 1024);
    }

    /**
     * Create directory (if not already exist)
     *
     * @param dir a directory to be created
     */
    public static void createDirectory(File dir) {
        if(!dir.exists()) {
            for (int i = 0; i < TEMP_DIR_CREATE_ATTEMPTS; i++) {
                if (dir.mkdir()) {
                    break;
                }
            }
            if(!dir.exists()) {
                throw new IllegalStateException("Cannot create temporary directory");
            }
        }
        dir.setWritable(true);
    }
}
