package org.artifactory.storage.binstore.utils;

import javax.annotation.Nonnull;

/**
 * @author gidis
 */
public class MaskUtils {
    /**
     * Replaces all the characters of the string with asterisks ('*').
     * <pre>
     * mask("acb") = "***"
     * mask("") = ""
     * mask(null) = ""
     * </pre>
     */
    @Nonnull
    public static String mask(String key, String value) {
        if (value == null) {
            return "";
        }
        if (shouldMaskValue(key)) {
            StringBuilder sb = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                sb.append('*');
            }
            return sb.toString();
        }
        return value;
    }


    public static boolean shouldMaskValue(String propertyKey) {
        String propKeyLower = propertyKey.toLowerCase();
        return propKeyLower.contains("s3proxycredential")
                || propKeyLower.contains("gsproxycredential")
                || propKeyLower.contains("credentials")
                || propKeyLower.contains("credential")
                || propKeyLower.contains("password")
                || propKeyLower.contains("secret")
                || propKeyLower.contains("key");
    }
}
