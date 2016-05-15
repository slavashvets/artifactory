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

package org.artifactory.post.services;

import com.google.common.collect.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.callhome.CallHomeRequest;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.jackson.JacksonWriter;
import org.artifactory.api.repo.Async;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.post.providers.features.CallHomeFeature;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.artifactory.common.ConstantValues.artifactoryVersion;

/**
 * @author Michael Pasternak
 */
@Service
public class CallHomeService {

    private static final Logger log = LoggerFactory.getLogger(CallHomeService.class);
    private static final String PARAM_OS_ARCH = "os.arch";
    private static final String PARAM_OS_NAME = "os.name";
    private static final String PARAM_JAVA_VERSION = "java.version";

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private List<CallHomeFeature> callHomeFeatures = Lists.newArrayList();

    @Autowired
    private AddonsManager addonsManager;

    /**
     * Sends post message with usage info to bintray
     */
    @Async
    public void callHome() {
        if (ConstantValues.versionQueryEnabled.getBoolean() && !configService.getDescriptor().isOfflineMode()) {
            try (CloseableHttpClient client = createHttpClient()) {
                String url = ConstantValues.bintrayApiUrl.getString() + "/products/jfrog/artifactory/stats/usage";
                HttpPost postMethod = new HttpPost(url);
                postMethod.setEntity(callHomeEntity());
                log.debug("Calling home...");
                client.execute(postMethod);
            } catch (Exception e) {
                log.debug("Failed calling home: " + e.getMessage(), e);
            }
        }
    }

    /**
     * @return {@link CloseableHttpClient}
     */
    private CloseableHttpClient createHttpClient() {
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        return new HttpClientConfigurator()
                .soTimeout(15000)
                .connectionTimeout(1500)
                .retry(0, false)
                .proxy(proxy)
                .getClient();
    }

    /**
     * Produces callHomeEntity
     *
     * @return {@link HttpEntity}
     *
     * @throws IOException on serialization errors
     */
    private HttpEntity callHomeEntity() throws IOException {
        CallHomeRequest request = new CallHomeRequest();

        addInstanceAndEnvInfo(request);
        addFeatures(request);
        return serializeToStringEntity(request);
    }

    /**
     * Serializes {@link CallHomeRequest} to {@link org.apache.http.entity.StringEntity}
     *
     * @param request {@link CallHomeRequest}
     *
     * @return {@link org.apache.http.entity.StringEntity}
     * @throws IOException happens if serialization fails
     */
    private StringEntity serializeToStringEntity(CallHomeRequest request) throws IOException {
        String serialized = JacksonWriter.serialize(request, true);
        return new StringEntity(serialized, ContentType.APPLICATION_JSON);
    }

    /**
     * Collects features metadata  {@see RTFACT-8412}
     *
     * @param request that holds the entire callhome content
     */
    private void addFeatures(CallHomeRequest request) {
        callHomeFeatures.stream()
                .map(CallHomeFeature::getFeature)
                .forEach(request::addCallHomeFeature);
    }

    /**
     * Collect environment and instance information
     * @param request that should the entire call home information
     */
    private void addInstanceAndEnvInfo(CallHomeRequest request){
        request.version = artifactoryVersion.getString();

        String license = getLicenseType();
        if (license != null && license.equals("aol")){
            boolean isDedicated = ArtifactoryHome.get().getArtifactoryProperties()
                    .getBooleanProperty(ConstantValues.aolDedicatedServer);
            license = isDedicated ? license + " dedicated" : license;
        }
        request.licenseType = license;

        request.licenseOEM = addonsManager.isPartnerLicense() ? "VMware" : null;
        Date licenseValidUntil = addonsManager.getLicenseValidUntil();
        if (licenseValidUntil != null) {
            request.licenseExpiration = ISODateTimeFormat.dateTime().print(new DateTime(licenseValidUntil));
        }
        request.setDist(System.getProperty("artdist"));
        request.environment.hostId = addonsManager.addonByType(HaCommonAddon.class).getHostId();
        request.environment.licenseHash = addonsManager.getLicenseKeyHash();
        request.environment.attributes.osName = System.getProperty(PARAM_OS_NAME);
        request.environment.attributes.osArch = System.getProperty(PARAM_OS_ARCH);
        request.environment.attributes.javaVersion = System.getProperty(PARAM_JAVA_VERSION);
    }


    private String getLicenseType() {
        if (addonsManager instanceof OssAddonsManager) {
            return "oss";
        }
        if (addonsManager.addonByType(CoreAddons.class).isAol()) {
            return "aol";
        } else if (addonsManager.getLicenseDetails()[2].equals("Trial")) {
            return "trial";
        } else if (addonsManager.getLicenseDetails()[2].equals("Commercial")) {
            return "pro";
        } else if (addonsManager.isHaLicensed()) {
            return "ent";
        }
        return null;
    }
}
