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

package org.artifactory.security.db.apikey;

import org.apache.commons.lang.StringUtils;
import org.artifactory.security.RealmAwareAuthenticationProvider;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.security.props.auth.BadPropsAuthException;
import org.artifactory.security.props.auth.PropsAuthenticationToken;
import org.artifactory.security.props.auth.PropsTokenCache;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationProvider implements RealmAwareAuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(PropsAuthenticationProvider.class);

    @Autowired
    private UserGroupStoreService userGroupStore;

    @Autowired
    private PropsTokenCache propsTokenCache;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(PropsAuthenticationToken.class,
                authentication, "Only Props Authentication Token is supported");
        PropsAuthenticationToken authToken = (PropsAuthenticationToken) authentication;
        // Determine props key and value
        String tokenKey = (String) authToken.getPropsKey();
        String tokenValue = (String) authentication.getCredentials();
        TokenKeyValue tokenKeyValue = new TokenKeyValue(tokenKey, tokenValue);
        UserDetails principal = findPrincipalByToken(tokenKeyValue);
        if (principal == null) {
            log.debug("token not found: {}", tokenKeyValue);
            throw new BadPropsAuthException("Bad props auth token: " + tokenKeyValue);
        } else {
            if (authToken.getPrincipal() != null && StringUtils.isNotBlank(authToken.getPrincipal().toString())) {
                if (!authToken.getPrincipal().toString().equals(principal.getUsername())) {
                    throw new BadPropsAuthException("Bad authentication Key " + tokenKeyValue
                            + " for user " + principal.getUsername());
                }
            }
        }
        return createSuccessAuthentication(authToken, principal);
    }

    private UserDetails findPrincipalByToken(TokenKeyValue tokenKeyValue) {
        UserDetails principal = propsTokenCache.get(tokenKeyValue);
        if (principal == null) {
            UserInfo user = userGroupStore.findUserByProperty(tokenKeyValue.getKey(), tokenKeyValue.getToken());
            if (user != null) {
                principal = new SimpleUser(user);
            }
        }
        return principal;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }

    protected Authentication createSuccessAuthentication(PropsAuthenticationToken authentication,
                                                         UserDetails user) {
        PropsAuthenticationToken result = new PropsAuthenticationToken(user, authentication.getPropsKey(),
                authentication.getCredentials(), new NullAuthoritiesMapper().mapAuthorities(user.getAuthorities()));

        result.setDetails(authentication.getDetails());
        result.setAuthenticated(true);
        return result;
    }

    @Override
    public void addExternalGroups(String username, Set<UserGroupInfo> groups) {
        // not require
    }

    @Override
    public boolean userExists(String username) {
        return userGroupStore.userExists(username);
    }

    @Override
    public String getRealm() {
        return null;
    }
}