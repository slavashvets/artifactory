package org.artifactory.api.bintray.distribution.resolver;

import java.util.regex.Pattern;

/**
 * <p>Created on 18/05/16
 *
 * @author Yinon Avraham
 */
public enum DistributionRuleFilterType {

    repo("repo"),
    path("path");

    public static final Pattern GENERAL_CAP_GROUP_PATTERN = createCapGroupPattern("[a-z]+");

    private final String qualifier;
    private final Pattern capGroupPattern;

    DistributionRuleFilterType(String qualifier) {
        this.qualifier = qualifier;
        this.capGroupPattern = createCapGroupPattern(qualifier);
    }

    private static Pattern createCapGroupPattern(String qualifier) {
        return Pattern.compile("(\\$\\{" + qualifier + ":\\d+\\})"); // ${qualifier:group_number}
    }

    public String getQualifier() {
        return qualifier;
    }

    public Pattern getCaptureGroupPattern() {
        return capGroupPattern;
    }

    /**
     * Tries to extract a capture group's number.
     *
     * @param group the captured group text, e.g. "${repo:1}"
     * @return the int value of the group
     */
    public int getGroupNumber(String group) throws NumberFormatException {
        int start = ("${" + qualifier + ":").length();
        return Integer.parseInt(group.substring(start, group.length()-1));
    }
}
