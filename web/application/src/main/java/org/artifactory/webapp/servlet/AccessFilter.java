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

import com.google.common.cache.CacheBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SecurityListener;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.security.HttpAuthenticationDetailsSource;
import org.artifactory.security.UserInfo;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.UiRequestUtils;
import org.artifactory.webapp.servlet.authentication.ArtifactoryAuthenticationFilter;
import org.artifactory.webapp.servlet.authentication.ArtifactoryAuthenticationFilterChain;
import org.artifactory.webapp.servlet.authentication.interceptor.anonymous.AnonymousAuthenticationInterceptor;
import org.artifactory.webapp.servlet.authentication.interceptor.anonymous.AnonymousAuthenticationInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class AccessFilter extends DelayedFilterBase implements SecurityListener {
    private static final Logger log = LoggerFactory.getLogger(AccessFilter.class);
    public static final String AUTHENTICATED_USERNAME_ATTRIBUTE = "authenticated_username";

    private ArtifactoryContext context;
    private ArtifactoryAuthenticationFilterChain authFilterChain;
    private BasicAuthenticationEntryPoint authenticationEntryPoint;
    private AnonymousAuthenticationInterceptors authInterceptors;

    /**
     * holds cached Authentication instances for the non ui requests based on the Authorization header and client ip
     */
    private ConcurrentMap<AuthCacheKey, Authentication> nonUiAuthCache;
    private ConcurrentMap<String, AuthenticationCache> userChangedCache;

    @Override
    public void initLater(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context = RequestUtils.getArtifactoryContext(servletContext);
        this.authenticationEntryPoint = context.beanForType(BasicAuthenticationEntryPoint.class);
        this.authFilterChain = new ArtifactoryAuthenticationFilterChain(authenticationEntryPoint);
        // Add all the authentication filters
        authFilterChain.addFilters(context.beansForType(ArtifactoryAuthenticationFilter.class).values());
        initCaches(filterConfig);
        authFilterChain.init(filterConfig);
        authInterceptors = new AnonymousAuthenticationInterceptors();
        RequestUtils.setPackagesEndpointUseBasicAuth();
        authInterceptors.addInterceptors(context.beansForType(AnonymousAuthenticationInterceptor.class).values());
    }

    private void initCaches(FilterConfig filterConfig) {
        ArtifactorySystemProperties properties =
                ((ArtifactoryHome) filterConfig.getServletContext().getAttribute(ArtifactoryHome.SERVLET_CTX_ATTR))
                        .getArtifactoryProperties();
        ConstantValues idleTimeSecsProp = ConstantValues.securityAuthenticationCacheIdleTimeSecs;
        long cacheIdleSecs = properties.getLongProperty(idleTimeSecsProp);
        ConstantValues initSizeProp = ConstantValues.securityAuthenticationCacheInitSize;
        long initSize = properties.getLongProperty(initSizeProp);
        nonUiAuthCache = CacheBuilder.newBuilder().softValues()
                .initialCapacity((int) initSize)
                .expireAfterWrite(cacheIdleSecs, TimeUnit.SECONDS)
                .<AuthCacheKey, Authentication>build().asMap();
        userChangedCache = CacheBuilder.newBuilder().softValues()
                .initialCapacity((int) initSize)
                .expireAfterWrite(cacheIdleSecs, TimeUnit.SECONDS)
                .<String, AuthenticationCache>build().asMap();
        SecurityService securityService = context.beanForType(SecurityService.class);
        securityService.addListener(this);
    }

    @Override
    public void onClearSecurity() {
        nonUiAuthCache.clear();
        userChangedCache.clear();
    }

    @Override
    public void onUserUpdate(String username) {
        invalidateUserAuthCache(username);
    }

    @Override
    public void onUserDelete(String username) {
        invalidateUserAuthCache(username);
    }

    private void invalidateUserAuthCache(String username) {
        // Flag change to force re-login
        AuthenticationCache authenticationCache = userChangedCache.get(username);
        if (authenticationCache != null) {
            authenticationCache.changed();
        }
    }

    @Override
    public void destroy() {
        //May not be inited yet
        if (authFilterChain != null) {
            authFilterChain.destroy();
        }
        if (nonUiAuthCache != null) {
            nonUiAuthCache.clear();
            nonUiAuthCache = null;
        }
        if (userChangedCache != null) {
            userChangedCache.clear();
            userChangedCache = null;
        }
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException {
        doFilterInternal((HttpServletRequest) req, ((HttpServletResponse) resp), chain);
    }

    private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String servletPath = RequestUtils.getServletPathFromRequest(request);
        // add no cache header to web app request
        RequestUtils.addAdditionalHeadersToWebAppRequest(request, response);
        String method = request.getMethod();
        if ((servletPath == null || "/".equals(servletPath) || servletPath.length() == 0) &&
                "get".equalsIgnoreCase(method)) {
            //We were called with an empty path - redirect to the app main page
            response.sendRedirect(HttpUtils.WEBAPP_URL_PATH_PREFIX + "/");
            return;
        }
        // Reuse the authentication if it exists
        Authentication authentication = RequestUtils.getAuthentication(request);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        // Find the good filter chain for this request
        ArtifactoryAuthenticationFilter authenticationFilter = authFilterChain.acceptFilter(request);

        // Make sure this is called only once (FRED: it's called twice ?!?)
        boolean reAuthRequired = reAuthenticationRequired(request, authentication, authenticationFilter);
        if (reAuthRequired) {
            /**
             * A re-authentication is required but we might still have data that needs to be invalidated (like the
             * web session)
             */
            Map<String, LogoutHandler> logoutHandlers = ContextHelper.get().beansForType(LogoutHandler.class);
            for (LogoutHandler logoutHandler : logoutHandlers.values()) {
                logoutHandler.logout(request, response, authentication);
            }
        }
        boolean authenticationRequired = !isAuthenticated || reAuthRequired;
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (authenticationRequired) {
            if (authenticationFilter != null && authenticationFilter.acceptFilter(request)) {
                authenticateAndExecute(request, response, chain, securityContext, authenticationFilter, authFilterChain);
            } else {
                useAnonymousIfPossible(request, response, chain, securityContext, authenticationFilter);
            }
        } else {
            log.debug("Using authentication {} from Http session.", authentication);
            useAuthentication(request, response, chain, authentication, securityContext);
        }
    }

    private boolean reAuthenticationRequired(HttpServletRequest request, Authentication authentication,
            ArtifactoryAuthenticationFilter authenticationFilter) {
        // Not authenticated so not required to redo ;-)
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // If the user object changed in the DB: new groups or became admin since last login,
        // Then we need to force re-authentication
        String username = authentication.getName();
        AuthenticationCache authenticationCache = userChangedCache.get(username);
        if (authenticationCache != null && authenticationCache.isChanged(authentication)) {
            authenticationCache.loggedOut(authentication);
            return true;
        }
        if (isAuthenticatedUIRequest(request, authenticationFilter)) {
            return false;
        }
        if (authenticationFilter != null) {
            // Ask the filter chain if we need to re authenticate
            return authenticationFilter.requiresReAuthentication(request, authentication);
        } else {
            return true;
        }
    }

    /**
     *  if request related to UI and already authenticated the it do not require re-authentication
     * @param request - http servlet request
     * @param authenticationFilter - accepted authentication filter if null no filter accept this request
     * @return true if require authentication
     */
    private boolean isAuthenticatedUIRequest(HttpServletRequest request,
            ArtifactoryAuthenticationFilter authenticationFilter) {
        return UiRequestUtils.isUiRestRequest(request) ||
                ((request.getRequestURI().indexOf("webapp") != -1) && authenticationFilter == null) ||
                authenticationFilter == null;
    }


    private void authenticateAndExecute(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext,
            ArtifactoryAuthenticationFilter authFilter, ArtifactoryAuthenticationFilterChain authFilterChain)
            throws IOException, ServletException {
        // Try to see if authentication in cache based on the hashed header and client ip
        String cacheKey = authFilter.getCacheKey(request);
        log.debug("'{}' Cached key has been found for request: '{}' with method: '{}'", cacheKey,
                request.getRequestURI(), request.getMethod());
        AuthCacheKey authCacheKey = new AuthCacheKey(cacheKey, request.getRemoteAddr());
        Authentication authentication = getNonUiCachedAuthentication(request, authCacheKey);
        if (authentication != null && authentication.isAuthenticated()
                && !reAuthenticationRequired(request, authentication, authFilter)) {
            log.debug("Header authentication {} found in cache.", authentication);
            useAuthentication(request, response, chain, authentication, securityContext);
            // Add to user change cache the login state
            addToUserChange(authentication);
            return;
        }
        try {
            authFilterChain.doFilter(request, response, authFilter, chain);
        } finally {
            String username = "non_authenticated_user";
            Authentication newAuthentication = securityContext.getAuthentication();
            if (newAuthentication != null && newAuthentication.isAuthenticated()) {
                // Add to user change cache the login state
                addToUserChange(newAuthentication);
                // Save authentication (if session exists)
                if (RequestUtils.setAuthentication(request, newAuthentication, false)) {
                    log.debug("Added authentication {} in Http session.", newAuthentication);
                    username = newAuthentication.getName();
                } else {
                    // If it did not work use the header cache
                    // An authorization cache key with no header can only be used for Anonymous authentication
                    username = newAuthentication.getName();
                    if ((UserInfo.ANONYMOUS.equals(username) && authCacheKey.hasEmptyHeader()) ||
                            (!UserInfo.ANONYMOUS.equals(username) && !authCacheKey.hasEmptyHeader())) {
                        nonUiAuthCache.put(authCacheKey, newAuthentication);
                        userChangedCache.get(username).addAuthCacheKey(authCacheKey);
                        log.debug("Added authentication {} in cache.", newAuthentication);
                    }
                }
            }
            securityContext.setAuthentication(null);
            request.setAttribute(AUTHENTICATED_USERNAME_ATTRIBUTE, username);
        }
    }

    private void addToUserChange(Authentication authentication) {
        String username = authentication.getName();
        if (!UserInfo.ANONYMOUS.equals(username)) {
            AuthenticationCache existingCache = userChangedCache.putIfAbsent(username,
                    new AuthenticationCache(authentication));
            if (existingCache != null) {
                existingCache.loggedIn(authentication);
            }
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void useAnonymousIfPossible(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext,
            ArtifactoryAuthenticationFilter authFilter) throws IOException, ServletException {
        boolean anonAccessEnabled = context.getAuthorizationService().isAnonAccessEnabled();
        if ((anonAccessEnabled && !isAllowUIBuildsRequest(request, context.getCentralConfig())) || authInterceptors.accept(request)) {
            log.debug("Using anonymous");
            AuthCacheKey authCacheKey = getAuthCacheKey(request, authFilter);
            Authentication authentication = getNonUiCachedAuthentication(request, authCacheKey);
            if (authentication == null) {
                log.debug("Creating the Anonymous token");
                final UsernamePasswordAuthenticationToken authRequest =
                        new UsernamePasswordAuthenticationToken(UserInfo.ANONYMOUS, "");
                AuthenticationDetailsSource ads = new HttpAuthenticationDetailsSource();
                //noinspection unchecked
                authRequest.setDetails(ads.buildDetails(request));
                // explicitly ask for the default spring authentication manager by name (we have another one which
                // is only used by the basic authentication filter)
                AuthenticationManager authenticationManager =
                        context.beanForType("authenticationManager", AuthenticationManager.class);
                authentication = authenticationManager.authenticate(authRequest);
                if (authentication != null && authentication.isAuthenticated() && !RequestUtils.isUiRequest(request)) {
                    nonUiAuthCache.put(authCacheKey, authentication);
                    log.debug("Added anonymous authentication {} to cache", authentication);
                }
            } else {
                log.debug("Using cached anonymous authentication");
            }
            useAuthentication(request, response, chain, authentication, securityContext);
        } else {
            if (!RequestUtils.isUiRequest(request)) {
                log.debug("Sending request requiring authentication");
                authenticationEntryPoint.commence(request, response,
                        new InsufficientAuthenticationException("Authentication is required"));
            } else {
                log.debug("No filter or entry just chain");
                chain.doFilter(request, response);
            }
        }
    }

    private boolean isAllowUIBuildsRequest(HttpServletRequest request, CentralConfigService centralConfig) {
        // TODO: [by sy] should not be here like that, should be implemented as an aspect
        return request.getRequestURI().contains("/ui/builds/")
                && centralConfig.getDescriptor().getSecurity().isAnonAccessToBuildInfosDisabled();
    }

    private AuthCacheKey getAuthCacheKey(HttpServletRequest request,
            ArtifactoryAuthenticationFilter authFilter) {
        AuthCacheKey authCacheKey;
        if (authFilter != null) {
            authCacheKey = new AuthCacheKey(authFilter.getCacheKey(request), request.getRemoteAddr());
        } else {
            authCacheKey = new AuthCacheKey(null, request.getRemoteAddr());
        }
        return authCacheKey;
    }

    private Authentication getNonUiCachedAuthentication(HttpServletRequest request,
            AuthCacheKey authCacheKey) {
        // return cached authentication only if this is a non ui request (this guards the case when user accessed
        // Artifactory both from external tool and from the ui)
        return RequestUtils.isUiRequest(request) ? null : nonUiAuthCache.get(authCacheKey);
    }

    private void useAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authentication, SecurityContext securityContext) throws IOException, ServletException {
        try {
            securityContext.setAuthentication(authentication);
            chain.doFilter(request, response);
            addToUserChange(authentication);
        } finally {
            securityContext.setAuthentication(null);
            request.setAttribute(AUTHENTICATED_USERNAME_ATTRIBUTE,
                    authentication != null ? authentication.getName() : "non_authenticated_user");
        }
    }

    @Override
    public int compareTo(SecurityListener o) {
        return 0;
    }

    private static class AuthCacheKey {
        private static final String EMPTY_HEADER = DigestUtils.shaHex("");

        private final String hashedHeader;
        private final String ip;

        private AuthCacheKey(String header, String ip) {
            if (header == null) {
                this.hashedHeader = EMPTY_HEADER;
            } else {
                this.hashedHeader = DigestUtils.shaHex(header);
            }
            this.ip = ip;
        }

        public boolean hasEmptyHeader() {
            return this.hashedHeader.equals(EMPTY_HEADER);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuthCacheKey key = (AuthCacheKey) o;
            return hashedHeader.equals(key.hashedHeader) && ip.equals(key.ip);
        }

        @Override
        public int hashCode() {
            int result = hashedHeader.hashCode();
            result = 31 * result + ip.hashCode();
            return result;
        }
    }

    class AuthenticationCache {
        Set<AuthCacheKey> authCacheKeys;
        Map<Integer, Integer> authState = new HashMap<>(3);

        AuthenticationCache(Authentication first) {
            authState.put(first.hashCode(), 0);
        }

        synchronized void addAuthCacheKey(AuthCacheKey authCacheKey) {
            if (authCacheKeys == null) {
                authCacheKeys = new HashSet<>();
            }
            authCacheKeys.add(authCacheKey);
        }

        synchronized void changed() {
            if (authCacheKeys != null) {
                for (AuthCacheKey authCacheKey : authCacheKeys) {
                    Authentication removed = nonUiAuthCache.remove(authCacheKey);
                    if (removed != null) {
                        Integer key = removed.hashCode();
                        log.debug("Removed {}:{} from the non-ui authentication cache", removed.getName(), key);
                        authState.put(key, 1);
                    }
                }
                authCacheKeys.clear();
            }
            Set<Integer> keys = new HashSet<>(authState.keySet());
            for (Integer key : keys) {
                authState.put(key, 1);
            }
        }

        boolean isChanged(Authentication auth) {
            int key = auth.hashCode();
            Integer state = authState.get(key);
            if (state != null) {
                return state == 1;
            }
            return false;
        }

        void loggedOut(Authentication auth) {
            authState.put(auth.hashCode(), 2);
        }

        void loggedIn(Authentication auth) {
            authState.put(auth.hashCode(), 0);
        }
    }
}