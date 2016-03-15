package org.artifactory.request.range.stream;

import com.google.common.io.ByteStreams;
import org.artifactory.request.range.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Returns single sub-stream by wrapping the stream and skipping irrelevant bytes
 *
 * @author Gidi Shabat
 */
public class SingleRangeSkipInputStream extends FilterInputStream {
    public static final Logger log = LoggerFactory.getLogger(SingleRangeSkipInputStream.class);

    private static InputStream wrapInputStream(Range range, InputStream inputStream) throws IOException {
        // Skip irrelevant bytes
        ByteStreams.skipFully(inputStream, range.getStart());
        // Limit the stream
        return ByteStreams.limit(inputStream, range.getEnd() - range.getStart() + 1);
    }

    public SingleRangeSkipInputStream(Range range, InputStream inputStream) throws IOException {
        super(wrapInputStream(range, inputStream));
    }
}
