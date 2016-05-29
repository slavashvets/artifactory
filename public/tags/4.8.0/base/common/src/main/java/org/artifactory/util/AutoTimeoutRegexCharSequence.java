package org.artifactory.util;

/**
 * A {@link CharSequence} that has the ability to abort an operation running on it (i.e. regex matching) after the
 * specified {@param timeoutMillis} has passed.
 *
 * @author Dan Feldman
 */
public class AutoTimeoutRegexCharSequence implements CharSequence {

    private final CharSequence inner;
    private final int timeoutMillis;
    private final long timeoutTime;
    private final String stringToMatch;
    private final String regexPattern;

    public AutoTimeoutRegexCharSequence(CharSequence inner, String stringToMatch, String regexPattern, int timeoutMillis) {
        super();
        this.inner = inner;
        this.timeoutMillis = timeoutMillis;
        this.stringToMatch = stringToMatch;
        this.regexPattern = regexPattern;
        timeoutTime = System.currentTimeMillis() + timeoutMillis;
    }

    public char charAt(int index) {
        //TODO [by dan]: currenTime is costly... can also count how many charAt calls were made but is should be something big
        long currentTime = System.currentTimeMillis();
        if (currentTime > timeoutTime) {
            throw new RuntimeException("Timeout occurred after " + (currentTime - timeoutMillis) + " ms while " +
                    "processing regex '" + regexPattern + "' on input '" + stringToMatch);
        }
        return inner.charAt(index);
    }

    public int length() {
        return inner.length();
    }

    public CharSequence subSequence(int start, int end) {
        return new AutoTimeoutRegexCharSequence(inner.subSequence(start, end), stringToMatch, regexPattern, timeoutMillis);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
