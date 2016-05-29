package org.artifactory.descriptor.repo.distribution;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.BintrayApplicationConfig;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@XmlType(name = "DistributionRepoType",
        propOrder = {"bintrayApplication", "rules", "proxy", "productName", "defaultNewRepoPrivate",
                "defaultNewRepoPremium", "defaultLicenses", "defaultVcsUrl", "whiteListedProperties",
                "gpgSign", "gpgPassPhrase"},
        namespace = Descriptor.NS)
public class DistributionRepoDescriptor extends LocalRepoDescriptor {

    @XmlIDREF
    @XmlElement(name = "bintrayApplicationRef")
    private BintrayApplicationConfig bintrayApplication;

    @XmlElement(name = "rule")
    @XmlElementWrapper(name = "rules")
    private List<DistributionRule> rules = Lists.newArrayList();

    @XmlIDREF
    @XmlElement(name = "proxyRef")
    private ProxyDescriptor proxy;

    @XmlElement
    private String productName;

    @XmlElement
    private boolean defaultNewRepoPrivate = true;

    @XmlElement
    private boolean defaultNewRepoPremium = true;

    @XmlElement(name = "license")
    @XmlElementWrapper(name = "defaultLicenses")
    private Set<String> defaultLicenses = new HashSet<>();

    @XmlElement
    private String defaultVcsUrl;

    @XmlElement(name = "property")
    @XmlElementWrapper(name = "whiteListedProperties")
    private Set<String> whiteListedProperties = new HashSet<>();

    @XmlElement
    private boolean gpgSign;

    @XmlElement
    private String gpgPassPhrase;

    @Override
    public boolean isHandleReleases() {
        return true;
    }

    @Override
    public boolean isHandleSnapshots() {
        return false;
    }

    public BintrayApplicationConfig getBintrayApplication() {
        return bintrayApplication;
    }

    public void setBintrayApplication(BintrayApplicationConfig bintrayApplication) {
        this.bintrayApplication = bintrayApplication;
    }

    public List<DistributionRule> getRules() {
        return rules;
    }

    public void setRules(List<DistributionRule> rules) {
        this.rules = rules;
    }

    public ProxyDescriptor getProxy() {
        return proxy;
    }

    public void setProxy(ProxyDescriptor proxy) {
        this.proxy = proxy;
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

    public Set<String> getWhiteListedProperties() {
        return whiteListedProperties;
    }

    public void setWhiteListedProperties(Set<String> whiteListedProperties) {
        this.whiteListedProperties = whiteListedProperties;
    }

    public boolean isGpgSign() {
        return gpgSign;
    }

    public void setGpgSign(boolean gpgSign) {
        this.gpgSign = gpgSign;
    }

    public String getGpgPassPhrase() {
        return gpgPassPhrase;
    }

    public void setGpgPassPhrase(String gpgPassPhrase) {
        this.gpgPassPhrase = gpgPassPhrase;
    }

}
