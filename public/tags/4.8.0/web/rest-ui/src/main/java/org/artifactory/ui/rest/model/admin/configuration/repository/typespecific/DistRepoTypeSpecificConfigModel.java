package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistRepoTypeSpecificConfigModel implements TypeSpecificConfigModel {

    private String clientId;
    //The Bintray App Config associated with this repo - it's hidden for now but is interchangeable between repos
    private String bintrayAppConfig;

    /**
     * These are 'hidden' values used in creation - they are not visible to the user, and are not part of the config
     */
    private String bintrayAuthString; //The auth string Bintray returns, which is Base64(clientId:secret)
    private String paramClientId;     //clientId that was passed as url param - for verification against the auth string
    private String code;              //The temp code Bintray returns after oauth app setup in the redirect request
    private String scope;               //The scope field sent back by Bintray which signifies the org this app has been created for
    private String redirectUrl;       //The redirect url the UI gave when executing the initial request - used for verification by Bintray
    //The base url UI should use when going to Bintray - mainly for our testing purposes so we can go to staging
    private String bintrayBaseUrl = ConstantValues.bintrayUrl.getString();
    //Need this here because we populate custom licenses for the specific org as well
    List<String> availableLicenses;
    private boolean isPremium;
    private String org;
    private Boolean isAuthenticated;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getBintrayAppConfig() {
        return bintrayAppConfig;
    }

    public void setBintrayAppConfig(String bintrayAppConfig) {
        this.bintrayAppConfig = bintrayAppConfig;
    }

    public String getBintrayAuthString() {
        return bintrayAuthString;
    }

    public void setBintrayAuthString(String bintrayAuthString) {
        this.bintrayAuthString = bintrayAuthString;
    }

    public String getParamClientId() {
        return paramClientId;
    }

    public void setParamClientId(String paramClientId) {
        this.paramClientId = paramClientId;
    }

    public String getBintrayBaseUrl() {
        return bintrayBaseUrl;
    }

    public void setBintrayBaseUrl(String bintrayBaseUrl) {
        this.bintrayBaseUrl = bintrayBaseUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public List<String> getAvailableLicenses() {
        return availableLicenses;
    }

    public void setAvailableLicenses(List<String> availableLicenses) {
        this.availableLicenses = availableLicenses;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public Boolean getAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Distribution;
    }

    @Override
    public String getUrl() {
        return StringUtils.EMPTY;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
