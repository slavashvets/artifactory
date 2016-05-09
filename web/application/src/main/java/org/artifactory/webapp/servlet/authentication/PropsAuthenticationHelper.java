package org.artifactory.webapp.servlet.authentication;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.SecurityService;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationHelper {

    private static final Logger log = LoggerFactory.getLogger(PropsAuthenticationHelper.class);

    public static TokenKeyValue getTokenKeyValueFromHeader(HttpServletRequest request) {
        TokenKeyValue tokenKeyValue;
        // 1st check if api key token exist
        if ((tokenKeyValue = getApiKeyTokenKeyValue(request)) != null) {
            return tokenKeyValue;
            // 2nd check if oauth token exist
        }
        if ((tokenKeyValue = getOauthTokenKeyValue(request)) != null) {
            return tokenKeyValue;
        }
        return null;
    }

    /**
     * check weather api key is found on request
     *
     * @param request - http servlet request
     * @return Token key value
     */
    private static TokenKeyValue getApiKeyTokenKeyValue(HttpServletRequest request) {
        String apiKeyValue = request.getHeader(ApiKeyManager.API_KEY_HEADER);
        if (StringUtils.isBlank(apiKeyValue)) {
            apiKeyValue = request.getHeader(ApiKeyManager.OLD_API_KEY_HEADER);
        }
        if (apiKeyValue != null) {
            return new TokenKeyValue(ApiKeyManager.API_KEY, apiKeyValue);
        }
        return null;
    }

    /**
     * check weather oauth key is found on request
     *
     * @param request - http servlet request
     * @return Token key value
     */
    private static TokenKeyValue getOauthTokenKeyValue(HttpServletRequest request) {
        String oauthToken = request.getHeader(OauthManager.AUTHORIZATION_HEADER);
        if (oauthToken != null && oauthToken.startsWith(OauthManager.OAUTH_TOKEN_PREFIX) && oauthToken.length() > 8) {
            oauthToken = oauthToken.substring(7);
            return new TokenKeyValue(OauthManager.OAUTH_KEY, oauthToken);
        }
        return null;
    }

    /**
     * Extracts the remote user name from the request, either from an attribute/header whose name was defined in the
     * SSO config or from the {@link javax.servlet.http.HttpServletRequest#getRemoteUser()} method.
     *
     * @param request HTTP request
     * @return Remote user name if found. Null or blank if not.
     */
    public static String getRemoteUserName(SecurityService securityService, HttpServletRequest request) {
        log.debug("Entering ArtifactorySsoAuthenticationFilter.getRemoteUserName");

        String ssoUserName = null;

        String requestVariable = securityService.getHttpSsoRemoteUserRequestVariable();
        if (StringUtils.isNotBlank(requestVariable)) {
            log.debug("Remote user request variable = '{}'.", requestVariable);
            // first attempt to read from attribute (to support custom filters)
            Object userAttribute = request.getAttribute(requestVariable);
            if (userAttribute != null) {
                ssoUserName = userAttribute.toString();
                log.debug("Remote user attribute: '{}'.", ssoUserName);
            }

            if (StringUtils.isBlank(ssoUserName)) {
                // check if the container got the remote user (e.g., using ajp)
                ssoUserName = request.getRemoteUser();
                log.debug("Remote user from request: '{}'.", ssoUserName);
            }

            if (StringUtils.isBlank(ssoUserName)) {
                // check if the request header contains the remote user
                ssoUserName = request.getHeader(requestVariable);
                log.debug("Remote user from header: '{}'.", ssoUserName);
            }
        }
        return ssoUserName != null ? ssoUserName.toLowerCase() : null;
    }
}
