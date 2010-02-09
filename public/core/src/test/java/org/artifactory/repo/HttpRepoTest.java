/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Yoav Landman
 */
public class HttpRepoTest {
    private InternalRepositoryService internalRepoService;

    @BeforeClass
    public void setup() {
        System.setProperty(ConstantValues.artifactoryVersion.getPropertyName(), "test");
        internalRepoService = EasyMock.createMock(InternalRepositoryService.class);
        ArtifactoryContext contextMock = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(contextMock.beanForType(MetadataService.class))
                .andReturn(EasyMock.createMock(MetadataService.class));
        ArtifactoryContextThreadBinder.bind(contextMock);
        EasyMock.replay(contextMock);
        ArtifactorySystemProperties artifactorySystemProperties = new ArtifactorySystemProperties();
        artifactorySystemProperties.loadArtifactorySystemProperties(null, null);
        ArtifactorySystemProperties.bind(artifactorySystemProperties);
    }

    @AfterClass
    public void tearDown() {
        ArtifactoryContextThreadBinder.unbind();
    }

    @Test
    public void testProxyRemoteAuthAndMultihome() {
        ProxyDescriptor proxyDescriptor = new ProxyDescriptor();
        proxyDescriptor.setHost("proxyHost");
        proxyDescriptor.setUsername("proxy-username");
        proxyDescriptor.setPassword("proxy-password");

        HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
        httpRepoDescriptor.setUrl("http://test");

        httpRepoDescriptor.setProxy(proxyDescriptor);

        httpRepoDescriptor.setUsername("repo-username");
        httpRepoDescriptor.setPassword("repo-password");

        httpRepoDescriptor.setLocalAddress("0.0.0.0");

        HttpRepo httpRepo = new HttpRepo(internalRepoService, httpRepoDescriptor, false, null);
        HttpClient client = httpRepo.createHttpClient();

        Credentials proxyCredentials = client.getState().getProxyCredentials(AuthScope.ANY);
        Assert.assertNotNull(proxyCredentials);
        Assert.assertTrue(proxyCredentials instanceof UsernamePasswordCredentials,
                "proxyCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) proxyCredentials).getUserName(), "proxy-username");
        Assert.assertEquals(((UsernamePasswordCredentials) proxyCredentials).getPassword(), "proxy-password");

        Credentials repoCredentials = client.getState().getCredentials(
                new AuthScope("test", AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        Assert.assertNotNull(repoCredentials);
        Assert.assertTrue(repoCredentials instanceof UsernamePasswordCredentials,
                "repoCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) repoCredentials).getUserName(), "repo-username");
        Assert.assertEquals(((UsernamePasswordCredentials) repoCredentials).getPassword(), "repo-password");

        Assert.assertEquals(client.getHostConfiguration().getLocalAddress().getHostAddress(), "0.0.0.0");
    }
}