package org.artifactory.security.props.auth.model;

import java.io.Serializable;

/**
 * @author Chen Keinan
 */
public class TokenKeyValue implements Serializable {

    private String token;

    private String key;

    public TokenKeyValue(String token) {
        this.token = token;
    }

    public TokenKeyValue(String key, String token) {
        this.key = key;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return String.format("%s=%s", key, token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenKeyValue that = (TokenKeyValue) o;

        return this.toString().equals(that.toString());

    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
