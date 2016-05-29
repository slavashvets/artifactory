package org.artifactory.request;

import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.request.range.RangeAwareContext;
import org.jfrog.storage.binstore.ifc.SkippableInputStream;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.artifactory.request.range.ResponseWithRangeSupportHelper.createRangeAwareContext;

/**
 * @author Gidi Shabat
 */
@Test
public class ResponseWithHttpRangeSupportHelperTest {
    private byte[] content;

    @BeforeClass
    public void init() {
        ArtifactoryHome.bind(new ArtifactoryHome(new File("./target/test/DebianEventsTest")));
        content = "0123456789ABCDEFGI".getBytes();
    }

    @Test
    public void noRangeRequest() throws IOException {
        String range = null;
        testDummyRangesReturnAll(range, true);
        testDummyRangesReturnAll(range, false);
    }

    @Test
    public void emptyRangeRequest() throws IOException {
        String range = "";
        testDummyRangesReturnAll(range, true);
        testDummyRangesReturnAll(range, false);
    }

    @Test
    public void invalidRangeRequest() throws IOException {
        String range = "ranges:a-b";
        testDummyRangesReturnAll(range, true);
        testDummyRangesReturnAll(range, false);
    }

    @Test
    public void singleRangeRequest() throws IOException {
        for (int first = 0; first < content.length - 1; first++) {
            for (int last = first; last < content.length; last++) {
                String range = "byte=" + first + "-" + last;
                testSingleRangeFirstLast(range, first, last);
            }
            // Test with start only range
            String range = "byte=" + first + "-";
            testSingleRangeFirstLast(range, first, content.length - 1);
        }
    }

    @Test
    public void multiRangeRequest() throws IOException {
        String range = "bytes=1-3,7-10";
        testMultiRangeRequest(range, true);
        testMultiRangeRequest(range, false);
    }

    @Test
    public void multiRangeDescendingOrderRequest() throws IOException {
        String range = "bytes=7-10,1-3";
        testMultiRangeRequest(range, true);
        testMultiRangeRequest(range, false);
    }

    @Test
    public void multiWithPartialErrorRangeDescendingOrderRequest() throws IOException {
        String range = "bytes=7-10,12-a,1-3";
        testMultiRangeRequest(range, true);
        testMultiRangeRequest(range, false);
    }

    @Test
    public void multiWithMergeRangeDescendingOrderRequest() throws IOException {
        String range = "bytes=7-10,1-6";
        testSingleRangeFirstLast(range, 1, 10);
    }

    private void assertSameContentNoStatus(RangeAwareContext context, String suffixMsg) throws IOException {
        // Assert content length
        Assert.assertEquals(content.length, context.getContentLength(), "Wrong content length for " + suffixMsg);
        // Assert content
        byte[] resultContent = IOUtils.toByteArray(context.getInputStream());
        Assert.assertEquals(resultContent, content, "Expecting no change in the content for " + suffixMsg);
        //Assert status
        Assert.assertEquals(context.getStatus(), -1, "Expecting no status for " + suffixMsg);
        //Assert etag extension
        Assert.assertEquals(context.getEtagExtension(), "", "Expecting no etag to be set for " + suffixMsg);
        // Assert content type
        Assert.assertEquals(context.getContentType(), "pdf", "Expecting no change in the content type for " + suffixMsg);
        // Assert content range
        Assert.assertEquals(context.getContentRange(), null, "Expecting no content range for " + suffixMsg);
    }

    private void testDummyRangesReturnAll(String range, boolean skippable) throws IOException {
        InputStream inputStream = createTestInputStream(skippable);
        RangeAwareContext context = createRangeAwareContext(inputStream, content.length, range, null, "pdf",
                -1, null);
        assertSameContentNoStatus(context, "range " + range + " skippable " + skippable);
    }

    private void testSingleRangeFirstLast(String range, int expectedFirst, int expectedLast) throws IOException {
        int expectedContentLength = expectedLast - expectedFirst + 1;
        byte[] expectedBytes = new byte[expectedContentLength];
        System.arraycopy(content, expectedFirst, expectedBytes, 0, expectedContentLength);
        int expectedStatus = 206;
        String expectedEtag = "-" + expectedFirst + "-" + expectedLast;
        String expectedContentRange = "bytes " + expectedFirst + "-" + expectedLast + "/" + content.length;

        internalTestSimpleRange(true, range, expectedContentLength, expectedBytes, expectedStatus, expectedEtag, expectedContentRange);
        internalTestSimpleRange(false, range, expectedContentLength, expectedBytes, expectedStatus, expectedEtag, expectedContentRange);
    }

    private void internalTestSimpleRange(boolean skippable,
                                         String range,
                                         int expectedContentLength,
                                         byte[] expectedBytes,
                                         int expectedStatus,
                                         String expectedEtag,
                                         String expectedContentRange) throws IOException {
        InputStream inputStream = createTestInputStream(skippable);
        RangeAwareContext context = createRangeAwareContext(inputStream, content.length, range, null, "pdf",
                -1, null);
        // Assert content length
        Assert.assertEquals(expectedContentLength, context.getContentLength(), "Wrong Length");
        // Assert content
        byte[] resultContent = IOUtils.toByteArray(context.getInputStream());
        Assert.assertEquals(resultContent, expectedBytes, "Wrong content for " + range + " skippable " + skippable);
        //Assert status
        Assert.assertEquals(context.getStatus(), expectedStatus, "Wrong status for " + range + " skippable " + skippable);
        //Assert etag extension
        Assert.assertEquals(context.getEtagExtension(), expectedEtag, "Wrong etag for " + range + " skippable " + skippable);
        // Assert content type
        Assert.assertEquals(context.getContentType(), "pdf", "Expecting no change in the content type for " + range + " skippable " + skippable);
        // Assert content range
        Assert.assertEquals(context.getContentRange(), expectedContentRange, "Wrong Content Range for " + range + " skippable " + skippable);
    }

    private void testMultiRangeRequest(String range, boolean skippable) throws IOException {
        InputStream inputStream = createTestInputStream(skippable);
        RangeAwareContext context = createRangeAwareContext(inputStream, content.length, range, null, "pdf",
                -1, null);
        String suffixMsg = "range " + range + " skippable " + skippable;
        // Assert content length
        Assert.assertEquals(context.getContentLength(), 196, "Wrong content length for " + suffixMsg);
        // Assert content
        byte[] resultContent = IOUtils.toByteArray(context.getInputStream());
        String expectedResult = "--BCD64322345343217845286A\r\n" +
                "Content-Type: pdf\r\n" +
                "Content-Range: bytes 1-3/18\r\n" +
                "\r\n" +
                "123\r\n" +
                "--BCD64322345343217845286A\r\n" +
                "Content-Type: pdf\r\n" +
                "Content-Range: bytes 7-10/18\r\n" +
                "\r\n" +
                "789A--BCD64322345343217845286A--\r\n";
        Assert.assertEquals(resultContent, expectedResult.getBytes(), "Wrong content for " + suffixMsg);
        //Assert status
        Assert.assertEquals(context.getStatus(), 206, "Wrong status for " + suffixMsg);
        //Assert etag extension
        Assert.assertEquals(context.getEtagExtension(), "-1-3-7-10", "Wrong etag for " + suffixMsg);
        // Assert content type
        Assert.assertEquals(context.getContentType(), "multipart/byteranges; boundary=BCD64322345343217845286A",
                "Wrong content type for " + suffixMsg);
        // Assert content range
        Assert.assertEquals(context.getContentRange(), null, "Wrong range for " + suffixMsg);
    }

    private InputStream createTestInputStream(boolean skippable) {
        InputStream inputStream;
        if (skippable) {
            inputStream = new ByteArrayInputStream(content);
        } else {
            inputStream = new NonSkippableStream(content);
        }
        return inputStream;
    }

    static class NonSkippableStream extends ByteArrayInputStream implements SkippableInputStream {
        public NonSkippableStream(byte[] buf) {
            super(buf);
        }

        @Override
        public synchronized long skip(long n) {
            throw new RuntimeException("I told you not to call me!");
        }

        @Override
        public boolean isSkippable() {
            return false;
        }
    }

}
