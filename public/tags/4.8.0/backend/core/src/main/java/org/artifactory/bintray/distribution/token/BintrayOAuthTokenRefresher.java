package org.artifactory.bintray.distribution.token;

import com.google.common.collect.Lists;
import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.handle.Bintray;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.bintray.BintrayTokenResponse;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.BintrayApplicationConfig;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.security.crypto.CryptoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.artifactory.bintray.distribution.util.DistributionUtils.getFormEncodedHeader;

/**
 * Queries Bintray for an OAuth token and saves the response refreshToken into the config descriptor if required.
 *
 * @author Shay Yaakov
 */
@Component
public class BintrayOAuthTokenRefresher {

    @Autowired
    CentralConfigService configService;

    @Autowired
    BintrayService bintrayService;

    public String refresh(DistributionRepoDescriptor descriptor) throws BintrayCallException {
        MutableCentralConfigDescriptor configDescriptor = configService.getMutableDescriptor();
        BintrayApplicationConfig config = getBintrayApplicationConfig(descriptor, configDescriptor);
        BintrayTokenResponse tokenResponse = executeRefreshTokenRequest(config, descriptor.getProxy());
        if (tokenResponse == null || !tokenResponse.isValid()) {
            String response = tokenResponse == null ? "empty" : tokenResponse.toString();
            throw new BintrayCallException(HttpStatus.SC_CONFLICT, "Failed to refresh and acquire token from Bintray",
                    "response is invalid: " + response);
        } else if (!config.getRefreshToken().equalsIgnoreCase(tokenResponse.refreshToken)) {
            config.setRefreshToken(tokenResponse.refreshToken);
            //Save descriptor with new Bintray app config only if refresh token has changed
            configService.saveEditedDescriptorAndReload(configDescriptor);
        }
        return Base64.encodeBase64String(tokenResponse.token.getBytes());
    }

    private BintrayTokenResponse executeRefreshTokenRequest(final BintrayApplicationConfig config,
            final ProxyDescriptor proxy) throws BintrayCallException {
        try (Bintray client = bintrayService.createBasicAuthBintrayClient(config.getClientId(), config.getSecret(), proxy, false)) {
            HttpResponse response = client.post("oauth/token", getFormEncodedHeader(), getTokenRefreshRequestFormParams(config));
            return JacksonReader.streamAsClass(response.getEntity().getContent(), BintrayTokenResponse.class);
        } catch (IOException ioe) {
            //IO can either be problem with streams or failure http return code
            if (ioe instanceof BintrayCallException) {
                throw (BintrayCallException) ioe;
            } else {
                throw new BintrayCallException(HttpStatus.SC_BAD_REQUEST, "Error executing refresh token request",
                        ioe.getMessage());
            }
        }
    }

    //params -> grant_type = refresh_token / client_id / refresh_token / scope
    private InputStream getTokenRefreshRequestFormParams(BintrayApplicationConfig config) {
        List<BasicNameValuePair> params = Lists.newArrayList(new BasicNameValuePair("grant_type", "refresh_token"),
                new BasicNameValuePair("client_id", config.getClientId()),
                new BasicNameValuePair("refresh_token", config.getRefreshToken()),
                new BasicNameValuePair("scope", config.getScope()));
        return IOUtils.toInputStream(URLEncodedUtils.format(params, "UTF-8"));
    }

    private BintrayApplicationConfig getBintrayApplicationConfig(DistributionRepoDescriptor descriptor,
            MutableCentralConfigDescriptor configDescriptor) throws BintrayCallException {
        String bintrayAppKey = descriptor.getBintrayApplication().getKey();
        BintrayApplicationConfig config = configDescriptor.getBintrayApplication(bintrayAppKey);
        if (config == null) {
            throw new BintrayCallException(HttpStatus.SC_NOT_FOUND, "Bintray Application config " + bintrayAppKey + " not found.", "");
        }
        decryptConfigIfNeeded(config);
        return config;
    }

    private void decryptConfigIfNeeded(BintrayApplicationConfig config) {
        config.setClientId(CryptoHelper.decryptIfNeeded(config.getClientId()));
        config.setSecret(CryptoHelper.decryptIfNeeded(config.getSecret()));
        config.setRefreshToken(CryptoHelper.decryptIfNeeded(config.getRefreshToken()));
    }
}
