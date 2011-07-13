/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.descriptor.security;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.group.LdapGroupSetting;
import org.artifactory.descriptor.security.sso.CrowdSettings;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.util.AlreadyExistsException;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yossi Shaul
 */
@XmlType(name = "SecurityType", propOrder = {"anonAccessEnabled", "hideUnauthorizedResources", "passwordSettings",
        "ldapSettings", "ldapGroupSettings", "httpSsoSettings", "crowdSettings"}, namespace = Descriptor.NS)
public class SecurityDescriptor implements Descriptor {

    @XmlElement(defaultValue = "true")
    private boolean anonAccessEnabled = true;

    /**
     * This flag controls whether we reveal the cause when user requests a resource she is not authorize to view.
     */
    @XmlElement(defaultValue = "false", required = false)
    private boolean hideUnauthorizedResources = false;

    @XmlElementWrapper(name = "ldapSettings")
    @XmlElement(name = "ldapSetting", required = false)
    private List<LdapSetting> ldapSettings = Lists.newArrayList();

    @XmlElementWrapper(name = "ldapGroupSettings")
    @XmlElement(name = "ldapGroupSetting", required = false)
    private List<LdapGroupSetting> ldapGroupSettings = Lists.newArrayList();

    @XmlElement(name = "passwordSettings", required = false)
    private PasswordSettings passwordSettings = new PasswordSettings();

    @XmlElement(name = "httpSsoSettings", required = false)
    private HttpSsoSettings httpSsoSettings;

    @XmlElement(name = "crowdSettings", required = false)
    private CrowdSettings crowdSettings;

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public void setAnonAccessEnabled(boolean anonAccessEnabled) {
        this.anonAccessEnabled = anonAccessEnabled;
    }

    /**
     * This flag controls whether we reveal the cause when user requests a resource she is not authorize to view.
     */
    public boolean isHideUnauthorizedResources() {
        return hideUnauthorizedResources;
    }

    /**
     * This flag controls whether we reveal the cause when user requests a resource she is not authorize to view.
     *
     * @param hideUnauthorizedResources If true hide security reason (return 404)
     */
    public void setHideUnauthorizedResources(boolean hideUnauthorizedResources) {
        this.hideUnauthorizedResources = hideUnauthorizedResources;
    }

    public List<LdapSetting> getLdapSettings() {
        return ldapSettings;
    }

    public void setLdapSettings(List<LdapSetting> ldapSettings) {
        if (ldapSettings != null) {
            this.ldapSettings = ldapSettings;
        } else {
            this.ldapSettings = Lists.newArrayList();
        }
    }

    public List<LdapGroupSetting> getLdapGroupSettings() {
        return ldapGroupSettings;
    }

    public void setLdapGroupSettings(List<LdapGroupSetting> ldapGroupSettings) {
        if (ldapGroupSettings != null) {
            this.ldapGroupSettings = ldapGroupSettings;
        } else {
            this.ldapGroupSettings = Lists.newArrayList();
        }
    }

    public void addLdap(LdapSetting ldapSetting) {
        if (ldapSettings.contains(ldapSetting)) {
            throw new AlreadyExistsException("The LDAP configuration " + ldapSetting.getKey() + " already exists");
        }
        ldapSettings.add(ldapSetting);
    }

    /**
     * When changing the LDAP settings configuration and enabling a new (or exisiting) LDAP settings, make sure the
     * other LDAP settings are not enabled.
     *
     * @param ldapSetting The LDAP setting that is updated.
     */
    public void ldapSettingChanged(LdapSetting ldapSetting) {
        LdapSetting setting = getLdapSettings(ldapSetting.getKey());
        if (setting != null) {
            int indexOfLdapSetting = ldapSettings.indexOf(ldapSetting);
            if (indexOfLdapSetting != -1) {
                ldapSettings.set(indexOfLdapSetting, ldapSetting);
            }
        }
    }


    public void ldapGroupSettingChanged(LdapGroupSetting ldapGroupSetting) {
        LdapGroupSetting groupSettings = getLdapGroupSettings(ldapGroupSetting.getName());
        if (groupSettings != null) {
            setLdapGroupSettings(ldapGroupSetting);
        }
    }

    public void addLdapGroup(LdapGroupSetting ldapGroupSetting) {
        if (ldapGroupSettings.contains(ldapGroupSetting)) {
            throw new AlreadyExistsException(
                    "The LDAP configuration " + ldapGroupSetting.getName() + " already exists");
        }
        ldapGroupSettings.add(ldapGroupSetting);
    }

    public LdapGroupSetting removeLdapGroup(String ldapGroupName) {
        LdapGroupSetting groupSettings = getLdapGroupSettings(ldapGroupName);
        if (groupSettings != null) {
            ldapGroupSettings.remove(groupSettings);
        }
        return groupSettings;
    }

    /**
     * In case an LDAP settings was removed, all LDAP groups with reference to the LDAP settings need to be
     * removed.
     *
     * @param ldapSettingKey The key of the LDAP settings being removed
     */
    public void removeLdapGroupsWithLdapSettingsKey(String ldapSettingKey) {
        List<LdapGroupSetting> settings = getLdapGroupSettings();
        Iterator<LdapGroupSetting> ldapGroupSettingIterator = settings.iterator();
        while (ldapGroupSettingIterator.hasNext()) {
            LdapGroupSetting setting = ldapGroupSettingIterator.next();
            if (setting.getEnabledLdap().equals(ldapSettingKey)) {
                ldapGroupSettingIterator.remove();
            }
        }
    }

    private LdapGroupSetting getLdapGroupSettings(String name) {
        for (LdapGroupSetting ldapGroupSetting : ldapGroupSettings) {
            if (ldapGroupSetting.getName().equals(name)) {
                return ldapGroupSetting;
            }
        }
        return null;
    }

    private void setLdapGroupSettings(LdapGroupSetting ldapGroupSetting) {
        if (!ldapGroupSettings.isEmpty()) {
            int indexOfLdapGroupSetting = ldapGroupSettings.indexOf(ldapGroupSetting);
            if (indexOfLdapGroupSetting != -1) {
                ldapGroupSettings.set(indexOfLdapGroupSetting, ldapGroupSetting);
            }
        }
    }

    public LdapSetting removeLdap(String ldapKey) {
        LdapSetting ldapSetting = getLdapSettings(ldapKey);
        if (ldapSetting == null) {
            return null;
        }

        ldapSettings.remove(ldapSetting);
        removeLdapGroupsWithLdapSettingsKey(ldapKey);

        return ldapSetting;
    }

    public LdapSetting getLdapSettings(String ldapKey) {
        for (LdapSetting ldap : ldapSettings) {
            if (ldap.getKey().equals(ldapKey)) {
                return ldap;
            }
        }
        return null;
    }

    public boolean isLdapExists(String key) {
        return getLdapSettings(key) != null;
    }

    public List<LdapSetting> getEnabledLdapSettings() {
        List<LdapSetting> result = Lists.newArrayList();
        for (LdapSetting ldap : ldapSettings) {
            if (ldap.isEnabled()) {
                result.add(ldap);
            }
        }
        return result;
    }

    public List<LdapGroupSetting> getEnabledLdapGroupSettings() {
        List<LdapGroupSetting> result = Lists.newArrayList();
        for (LdapGroupSetting groupSetting : ldapGroupSettings) {
            if (groupSetting.isEnabled()) {
                result.add(groupSetting);
            }
        }
        return result;
    }

    public boolean isLdapEnabled() {
        for (LdapSetting ldapSetting : ldapSettings) {
            if (ldapSetting.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public PasswordSettings getPasswordSettings() {
        return passwordSettings;
    }

    public HttpSsoSettings getHttpSsoSettings() {
        return httpSsoSettings;
    }

    public void setHttpSsoSettings(HttpSsoSettings httpSsoSettings) {
        this.httpSsoSettings = httpSsoSettings;
    }

    public CrowdSettings getCrowdSettings() {
        return crowdSettings;
    }

    public void setCrowdSettings(CrowdSettings crowdSettings) {
        this.crowdSettings = crowdSettings;
    }
}
