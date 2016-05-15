package org.artifactory.security;

import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.PropsTokenCache;
import org.artifactory.security.props.auth.model.AuthenticationModel;
import org.artifactory.security.props.auth.model.OauthModel;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.artifactory.util.dateUtils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Chen  Keinan
 */
@Component
public class LoginHandlerImpl implements LoginHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginHandlerImpl.class);

    @Autowired
    private OauthManager oauthManager;

    @Autowired
    private PropsTokenCache propsTokenCache;

    @Override
    public OauthModel doBasicAuthWithDb(String[] tokens,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) throws IOException, ParseException {
        assert tokens.length == 2;
        AuthenticationManager authenticationManager = ContextHelper.get().beanForType(AuthenticationManager.class);
        String username = tokens[0];
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, tokens[1]);
        authRequest.setDetails(authenticationDetailsSource);
        Authentication authenticate = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        TokenKeyValue tokenKeyValue = oauthManager.getToken(username);
        if (tokenKeyValue == null) {
            tokenKeyValue = oauthManager.createToken(username);
        }
        if (tokenKeyValue == null) {
            log.debug("could not create and persist token for authenticated user {}, storing generated token in shared cache.", username);
            tokenKeyValue = oauthManager.generateToken(username);
            if (tokenKeyValue != null) {
                propsTokenCache.put(tokenKeyValue, (UserDetails) authenticate.getPrincipal());
            } else {
                throw new RuntimeException("failed to generate token for authenticated user: " + username);
            }
        }
        String createdAt = DateUtils.formatBuildDate(System.currentTimeMillis());
        OauthModel oauthModel = new AuthenticationModel(tokenKeyValue.getToken(), createdAt);
        return oauthModel;
    }

    @Override
    public OauthModel doBasicAuthWithProvider(String header, String username) {
        OAuthHandler oAuthHandler = ContextHelper.get().beanForType(OAuthHandler.class);
        CentralConfigDescriptor descriptor = ContextHelper.get().getCentralConfig().getDescriptor();
        OAuthSettings oauthSettings = descriptor.getSecurity().getOauthSettings();
        String defaultProvider = oauthSettings.getDefaultNpm();
        // try to get token from provider
        OauthModel oauthModel = oAuthHandler.getCreateToken(defaultProvider, username, header);
        return oauthModel;
    }

    @Override
    public String[] extractAndDecodeHeader(String header) throws IOException {

        byte[] base64Token = header.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = org.springframework.security.crypto.codec.Base64.decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token = new String(decoded, "UTF-8");

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }
}
