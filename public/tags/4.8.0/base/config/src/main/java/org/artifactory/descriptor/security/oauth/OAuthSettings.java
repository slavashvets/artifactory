package org.artifactory.descriptor.security.oauth;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Set;

/**
 * @author Gidi Shabat
 */
@XmlType(name = "oauthSettingsType",
        propOrder = {"enableIntegration", "allowUserToAccessProfile", "persistUsers", "defaultNpm", "oauthProvidersSettings"},
        namespace = Descriptor.NS)
public class OAuthSettings implements Descriptor {

    private Boolean enableIntegration = false;
    @XmlElement(defaultValue = "false")
    private boolean allowUserToAccessProfile = false;
    private Boolean persistUsers = false;
    private String defaultNpm;

    @XmlElementWrapper(name = "oauthProvidersSettings")
    private List<OAuthProviderSettings> oauthProvidersSettings = Lists.newArrayList();


    public Boolean getEnableIntegration() {
        return enableIntegration;
    }

    public void setEnableIntegration(Boolean enableIntegration) {
        this.enableIntegration = enableIntegration;
    }


    public List<OAuthProviderSettings> getOauthProvidersSettings() {
        return oauthProvidersSettings;
    }

    public void setOauthProvidersSettings(List<OAuthProviderSettings> oauthProvidersSettings) {
        this.oauthProvidersSettings = oauthProvidersSettings;
    }

    public String getDefaultNpm() {
        return defaultNpm;
    }

    public void setDefaultNpm(String defaultNpm) {
        this.defaultNpm = defaultNpm;
    }

    public Boolean getPersistUsers() {
        return persistUsers;
    }

    public void setPersistUsers(Boolean persistUsers) {
        this.persistUsers = persistUsers;
    }

    public boolean isAllowUserToAccessProfile() {
        return allowUserToAccessProfile;
    }

    public void setAllowUserToAccessProfile(boolean allowUserToAccessProfile) {
        this.allowUserToAccessProfile = allowUserToAccessProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuthSettings)) return false;

        OAuthSettings that = (OAuthSettings) o;

        if (isAllowUserToAccessProfile() != that.isAllowUserToAccessProfile()) return false;
        if (getEnableIntegration() != null ? !getEnableIntegration().equals(that.getEnableIntegration()) : that.getEnableIntegration() != null) return false;
        if (getPersistUsers() != null ? !getPersistUsers().equals(that.getPersistUsers()) : that.getPersistUsers() != null) return false;
        if (getDefaultNpm() != null ? !getDefaultNpm().equals(that.getDefaultNpm()) : that.getDefaultNpm() != null) return false;
        return oauthProviderSettingsIdentical(this.getOauthProvidersSettings(), that.getOauthProvidersSettings());

    }

    @Override
    public int hashCode() {
        int result = getEnableIntegration() != null ? getEnableIntegration().hashCode() : 0;
        result = 31 * result + (isAllowUserToAccessProfile() ? 1 : 0);
        result = 31 * result + (getPersistUsers() != null ? getPersistUsers().hashCode() : 0);
        result = 31 * result + (getDefaultNpm() != null ? getDefaultNpm().hashCode() : 0);
        result = 31 * result + (getOauthProvidersSettings() != null ? getOauthProvidersSettings().hashCode() : 0);
        return result;
    }

    private boolean oauthProviderSettingsIdentical(List<OAuthProviderSettings> l1, List<OAuthProviderSettings> l2) {
        if(l1 == null && l2 == null || (l1 == l2)) {
            return true;
        } else if (l1 == null || l2 == null) {
            return false;
        } else if (l1.size() != l2.size()) {
            return false;
        }
        Set<OAuthProviderSettings> l1Set = Sets.newHashSet(l1);
        return l2.stream()
                .filter(providerSetting -> !l1Set.contains(providerSetting))
                .count() != 0;
    }
}
