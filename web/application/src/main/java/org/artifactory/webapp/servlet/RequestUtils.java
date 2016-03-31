/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.servlet;


import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.common.ConstantValues;
import org.artifactory.md.Properties;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH;

/**
 * User: freds Date: Aug 13, 2008 Time: 10:56:25 AM
 */
public abstract class RequestUtils {
    private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

    private static final Set<String> NON_UI_PATH_PREFIXES = new HashSet<>();
    private static final Set<String> UI_PATH_PREFIXES = new HashSet<>();
    public static final String LAST_USER_KEY = "artifactory:lastUserId";
    private static final String DEFAULT_ENCODING = "utf-8";

    private RequestUtils() {
        // utility class
    }

    public static void setNonUiPathPrefixes(Collection<String> uriPathPrefixes) {
        NON_UI_PATH_PREFIXES.clear();
        NON_UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static void setUiPathPrefixes(Collection<String> uriPathPrefixes) {
        UI_PATH_PREFIXES.clear();
        UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    @SuppressWarnings({"IfMayBeConditional"})
    public static String getContextPrefix(HttpServletRequest request) {
        String contextPrefix;
        String requestUri = request.getRequestURI();
        int contextPrefixEndIdx = requestUri.indexOf('/', 1);
        if (contextPrefixEndIdx > 0) {
            contextPrefix = requestUri.substring(1, contextPrefixEndIdx);
        } else {
            contextPrefix = "";
        }
        return contextPrefix;
    }

    public static boolean isRepoRequest(HttpServletRequest request) {
        return isRepoRequest(request, false);
    }

    public static boolean isRepoRequest(HttpServletRequest request, boolean warnIfRepoDoesNotExist) {
        String servletPath = getServletPathFromRequest(request);
        String pathPrefix = PathUtils.getFirstPathElement(servletPath);
        if (pathPrefix == null || pathPrefix.length() == 0) {
            return false;
        }
        if (ArtifactoryRequest.LIST_BROWSING_PATH.equals(pathPrefix)) {
            pathPrefix = PathUtils.getFirstPathElement(servletPath.substring("list/".length()));
        }
        if (ArtifactoryRequest.SIMPLE_BROWSING_PATH.equals(pathPrefix)) {
            pathPrefix = PathUtils.getFirstPathElement(servletPath.substring("simple/".length()));
        }
        if (UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        if (NON_UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        String repoKey = pathPrefix;
        //Support repository-level metadata requests
        repoKey = NamingUtils.stripMetadataFromPath(repoKey);
        //Strip any matrix params
        int paramsIdx = repoKey.indexOf(Properties.MATRIX_PARAMS_SEP);
        if (paramsIdx > 0) {
            repoKey = repoKey.substring(0, paramsIdx);
        }
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        Set<String> allRepos = repositoryService.getAllRepoKeys();
        try {
            repoKey = URLDecoder.decode(repoKey, "utf-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not decode repo key '" + repoKey + "' in utf-8");
            return false;
        }
        if (!allRepos.contains(repoKey)) {
            if (warnIfRepoDoesNotExist) {
                log.warn("Request " + servletPath + " should be a repo request and does not match any repo key");
            }
            return false;
        }
        return true;
    }

    public static boolean isWebdavRequest(HttpServletRequest request) {
        if (!isRepoRequest(request)) {
            return false;
        }
        if (WebdavService.WEBDAV_METHODS.contains(request.getMethod().toLowerCase(Locale.ENGLISH))) {
            return true;
        }
        String wagonProvider = request.getHeader("X-wagon-provider");
        return wagonProvider != null && wagonProvider.contains("webdav");
    }

    public static boolean isUiRequest(HttpServletRequest request) {
        if (isWebdavRequest(request)) {
            return false;
        }
        String pathPrefix = PathUtils.getFirstPathElement(getServletPathFromRequest(request));
        return isUiPathPrefix(pathPrefix);
    }

    public static boolean isUiPathPrefix(String pathPrefix) {
        if (UI_PATH_PREFIXES.contains(pathPrefix)) {
            return true;
        }
        if (NON_UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        return false;
    }

    public static boolean isReservedName(String pathPrefix) {
        return !(UI_PATH_PREFIXES.parallelStream().filter(pathPrefix::equalsIgnoreCase).count() == 0
                && NON_UI_PATH_PREFIXES.parallelStream().filter(pathPrefix::equalsIgnoreCase).count() == 0
                && !"list".equalsIgnoreCase(pathPrefix));
    }

    public static boolean isAuthHeaderPresent(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ")) {
            String auth = header.substring(6);
            return !"Og==".equals(auth);
        }

        return false;
    }

    public static Authentication getAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Authentication) session.getAttribute(LAST_USER_KEY);
    }

    public static boolean setAuthentication(HttpServletRequest request, Authentication authentication,
            boolean createSession) {
        HttpSession session = request.getSession(createSession);
        if (session == null) {
            return false;
        }
        session.setAttribute(LAST_USER_KEY, authentication);
        return true;
    }

    public static void removeAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(LAST_USER_KEY);
        }
    }

    /**
     * Returns the un-decoded servlet path from the request
     *
     * @param req The received request
     * @return String - Servlet path
     */
    public static String getServletPathFromRequest(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        if (StringUtils.isBlank(contextPath)) {
            return req.getRequestURI();
        }
        return req.getRequestURI().substring(contextPath.length());
    }

    /**
     * @param servletContext The servlet context
     * @return The artifactory spring context
     */
    public static ArtifactoryContext getArtifactoryContext(ServletContext servletContext) {
        return (ArtifactoryContext) servletContext.getAttribute(ArtifactoryContext.APPLICATION_CONTEXT_KEY);
    }

    public static RepoPath getRepoPath(HttpServletRequest servletRequest) {
        return (RepoPath) servletRequest.getAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH);
    }

    /**
     * Extract the username out of the request, by checking the the header for the {@code Authorization} and then if it
     * starts with {@code Basic} get it as a base 64 token and decode it.
     *
     * @param request The request to examine
     * @return The extracted username
     */
    public static String extractUsernameFromRequest(ServletRequest request) {
        String header = ((HttpServletRequest) request).getHeader("Authorization");
        if ((header != null) && header.startsWith("Basic ")) {
            String token;
            byte[] base64Token;
            try {
                base64Token = header.substring(6).getBytes(DEFAULT_ENCODING);
                token = new String(org.apache.commons.codec.binary.Base64.decodeBase64(base64Token), DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                log.info("the encoding is not supported");
                return EMPTY;
            }
            String username = EMPTY;
            int delim = token.indexOf(':');
            if (delim != -1) {
                username = token.substring(0, delim);
            }
            return username;
        }
        return EMPTY;
    }

    /**
     * add no cache and no store header to response in order to avoid java script caching on browser.
     * In addition adds a compatibility header for IE (where needed) to avoid default compatibility view for
     * intranet sites.
     *
     * @param request     - http servlet request
     * @param response    - http servlet response
     */
    public static void addAdditionalHeadersToWebAppRequest(HttpServletRequest request, HttpServletResponse response) {
        final String servletPath = RequestUtils.getServletPathFromRequest(request);
        if (servletPath.contains(HttpUtils.WEBAPP_URL_PATH_PREFIX)) {
            verifyExplorerUserAgentAndSetHeader(request, response);
            if (servletPath.endsWith("/app.html") || servletPath.equals("/webapp/")) {
                // don't store (cache) the app.html in the browser. other resources contain unique version identifier
                response.setHeader("Cache-Control", "no-store,max-age=0");
            }
            if (!ConstantValues.enableUiPagesInIframe.getBoolean()) {
                response.setHeader("X-FRAME-OPTIONS", "DENY");
            }
        }
    }

    /**
     * Verifies user agent is Internet Explorer according to:
     * https://msdn.microsoft.com/en-us/library/hh869301(v=vs.85).aspx
     * https://msdn.microsoft.com/en-us/library/ms537503(v=vs.85).aspx
     * http://www.useragentstring.com/pages/Internet%20Explorer/
     *
     * And adds the compatibility header to avoid explorer defaulting to IE7 mode when launching compatibility view.
     * see RTFACT-7928
     */
    private static void verifyExplorerUserAgentAndSetHeader(HttpServletRequest request, HttpServletResponse response) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if(isNewExplorer(userAgent)) {
            response.setHeader("X-UA-Compatible", "IE=Edge");
        }
    }

    private static boolean isNewExplorer(String userAgent) {
        return StringUtils.isNotEmpty(userAgent) && (userAgent.contains("MSIE") || userAgent.contains("Trident")
                || (userAgent.contains("Windows") && userAgent.contains("Edge"))
                || userAgent.contains("IEMobile"));
    }

    /**
     * return user name by props auth
     *
     * @param request - http servlet request
     * @return user name
     */
    public static String getUserNameByPropsAuth(HttpServletRequest request) {
        TokenKeyValue tokenKeyValue;
        if ((tokenKeyValue = getApiKeyTokenKeyValue(request)) != null) {
            String userByPropAuth = ContextHelper.get().getAuthorizationService().
                    findUserByPropAuth(tokenKeyValue.getKey(), tokenKeyValue.getToken());
            if (userByPropAuth != null) {
                return userByPropAuth;
            }
        }
        return null;
    }

    /**
     * check weather api key is found on request
     *
     * @param request - http servlet request
     * @return Token key value
     */
    public static TokenKeyValue getApiKeyTokenKeyValue(HttpServletRequest request) {
        String apiKeyValue = request.getHeader("X-Api-Key");
        if (apiKeyValue != null) {
            return new TokenKeyValue(ApiKeyManager.API_KEY, apiKeyValue);
        }
        return null;
    }
}
