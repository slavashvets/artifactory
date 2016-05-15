package org.artifactory.security.props.auth;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * @author Chen Keinan
 */
@Component
public abstract class PropsTokenManager implements TokenManager {
    private static final Logger log = LoggerFactory.getLogger(PropsTokenManager.class);

    @Autowired
    UserGroupService userGroupService;

    /**
     * return props key for each token type (oauth , apiKey and etc)
     *
     * @return key
     */
    protected abstract String getPropKey();


    @Override
    public TokenKeyValue generateToken(String userName) {
        TokenKeyValue token = null;
        String key = getPropKey();
        try {
            String value = CryptoHelper.generateUniqueToken();
            token = new TokenKeyValue(key, value);
        } catch (GeneralSecurityException e) {
            log.debug("error with generating token for user {} with key {}", userName, key, e);
        }
        return token;
    }

    @Override
    public TokenKeyValue createToken(String userName) {
        TokenKeyValue token = null;
        String tokenValue = null;
        String key = getPropKey();
        try {
            tokenValue = CryptoHelper.generateUniqueToken();
            boolean tokenPropCreated = userGroupService.createPropsToken(userName, key, tokenValue);
            if (tokenPropCreated) {
                token = new TokenKeyValue(key, tokenValue);
            }
        } catch (GeneralSecurityException e) {
            log.debug("error with generating token for user {} with key {}", userName, key, e);
        } catch (SQLException e) {
            log.debug("error with adding token for user {} with key {} and value {}", userName, key, tokenValue, e);
        }
        return token;
    }

    @Override
    public TokenKeyValue addExternalToken(String userName, String tokenValue) {
        TokenKeyValue token = null;
        String key = getPropKey();
        try {
            boolean propsToken = userGroupService.createPropsToken(userName, key, tokenValue);
            if (propsToken) {
                token = new TokenKeyValue(key, tokenValue);
            }
        } catch (SQLException e) {
            log.debug("error with adding external token for user {} with key {} and value {}", userName, key, tokenValue, e);
        }
        return token;
    }
    @Override
    public TokenKeyValue refreshToken(String userName) {
        TokenKeyValue token = null;
        try {
            String value = CryptoHelper.generateUniqueToken();
            token = updateToken(userName, value);
        } catch (GeneralSecurityException e) {
            log.debug("error with refreshing token for user {}", userName, e);
        }
        return token;
    }

    @Override
    public TokenKeyValue updateToken(String userName, String value) {
        String key = getPropKey();
        TokenKeyValue token = null;
        try {
            boolean propsToken = userGroupService.updatePropsToken(userName, key, value);
            if (propsToken) {
                token = new TokenKeyValue(key, value);
            }
        } catch (SQLException e) {
            log.debug("error with updating token for user {} with key {} and value {}", userName, key, value, e);
        }
        return token;
    }

    public TokenKeyValue getToken(String userName) {
        String key = getPropKey();
        TokenKeyValue token = null;
        String value = userGroupService.getPropsToken(userName, key);
        if (value != null) {
            token = new TokenKeyValue(key, value);
        }
        return token;
    }

    @Override
    public boolean revokeToken(String userName) {
        boolean tokenRevokeSucceeded = false;
        String key = getPropKey();
        try {
            tokenRevokeSucceeded = userGroupService.revokePropsToken(userName, key);
        } catch (SQLException e) {
            log.debug("error with revoking token for user {} with key {}", userName, key, e);
        }
        return tokenRevokeSucceeded;
    }

    @Override
    public boolean revokeAllTokens() {
        boolean tokenRevokeSucceeded = false;
        String key = getPropKey();
        try {
            userGroupService.revokeAllPropsTokens(key);
            tokenRevokeSucceeded = true;
        } catch (SQLException e) {
            log.debug("error with revoking all tokens with key {}", key, e);
        }
        return tokenRevokeSucceeded;
    }
}
