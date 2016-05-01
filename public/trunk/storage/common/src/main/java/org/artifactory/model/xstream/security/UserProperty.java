package org.artifactory.model.xstream.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.security.UserPropertyInfo;

/**
 * @author Gidi Shabat
 */
@XStreamAlias("userProperty")
public class UserProperty implements UserPropertyInfo {

    private final String propKey;
    private final String propValue;

    public UserProperty(String propKey, String propValue) {
        this.propKey = propKey;
        this.propValue = propValue;
    }

    @Override
    public String getPropKey() {
        return propKey;
    }

    @Override
    public String getPropValue() {
        return propValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserProperty that = (UserProperty) o;

        return propKey != null ? propKey.equals(that.propKey) : that.propKey == null;

    }

    @Override
    public int hashCode() {
        return propKey != null ? propKey.hashCode() : 0;
    }
}
