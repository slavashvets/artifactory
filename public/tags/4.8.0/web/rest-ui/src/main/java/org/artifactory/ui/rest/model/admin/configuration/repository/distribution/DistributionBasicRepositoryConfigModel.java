package org.artifactory.ui.rest.model.admin.configuration.repository.distribution;

import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.local.LocalBasicRepositoryConfigModel;

import java.util.HashSet;
import java.util.Set;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.*;

/**
 * @author Dan Feldman
 */
public class DistributionBasicRepositoryConfigModel extends LocalBasicRepositoryConfigModel {

    protected String layout = DEFAULT_REPO_LAYOUT;
    private String productName;
    private boolean defaultNewRepoPrivate = DEFAULT_NEW_BINTRAY_REPO_PRIVATE;
    private boolean defaultNewRepoPremium = DEFAULT_NEW_BINTRAY_REPO_PREMIUM;
    private Set<String> defaultLicenses = new HashSet<>();
    private String defaultVcsUrl;

    @Override
    public String getLayout() {
        return layout;
    }

    @Override
    public void setLayout(String layout) {
        //Always generic
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public boolean getDefaultNewRepoPrivate() {
        return defaultNewRepoPrivate;
    }

    public void setDefaultNewRepoPrivate(boolean defaultNewRepoPrivate) {
        this.defaultNewRepoPrivate = defaultNewRepoPrivate;
    }

    public boolean getDefaultNewRepoPremium() {
        return defaultNewRepoPremium;
    }

    public void setDefaultNewRepoPremium(boolean defaultNewRepoPremium) {
        this.defaultNewRepoPremium = defaultNewRepoPremium;
    }

    public Set<String> getDefaultLicenses() {
        return defaultLicenses;
    }

    public void setDefaultLicenses(Set<String> defaultLicenses) {
        this.defaultLicenses = defaultLicenses;
    }

    public String getDefaultVcsUrl() {
        return defaultVcsUrl;
    }

    public void setDefaultVcsUrl(String defaultVcsUrl) {
        this.defaultVcsUrl = defaultVcsUrl;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
