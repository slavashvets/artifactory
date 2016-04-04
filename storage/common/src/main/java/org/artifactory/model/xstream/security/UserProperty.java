package org.artifactory.model.xstream.security;

import org.artifactory.security.UserPropertyInfo;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gidi Shabat
 */
@XStreamAlias("userProperty")
public class UserProperty implements UserPropertyInfo {

    //TODO: [by YS] user db id should not be here
    private final transient long userId;
    private final String propKey;
    private final String propValue;

    public UserProperty(long userId, String propKey, String propValue) {
        this.userId = userId;
        this.propKey = propKey;
        this.propValue = propValue;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getPropKey() {
        return propKey;
    }

    @Override
    public String getPropValue() {
        return propValue;
    }
}
