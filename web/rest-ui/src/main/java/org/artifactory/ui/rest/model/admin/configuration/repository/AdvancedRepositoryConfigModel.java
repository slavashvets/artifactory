package org.artifactory.ui.rest.model.admin.configuration.repository;

import org.artifactory.rest.common.model.RestModel;
import org.artifactory.ui.rest.model.admin.configuration.propertysets.PropertySetNameModel;
import org.artifactory.ui.rest.model.admin.configuration.reverseProxy.ReverseProxyRepoModel;

import java.util.List;

/**
 * @author Dan Feldman
 */
public interface AdvancedRepositoryConfigModel extends RestModel {

    List<PropertySetNameModel> getPropertySets();

    void setPropertySets(List<PropertySetNameModel> propertySets);

    Boolean isBlackedOut();

    void setBlackedOut(Boolean blackedOut);

    Boolean getAllowContentBrowsing();

    void setAllowContentBrowsing(Boolean allowContentBrowsing);

    ReverseProxyRepoModel getReverseProxy();

    void setReverseProxy(ReverseProxyRepoModel reverseProxy);

    String toString();
}
