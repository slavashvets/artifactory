package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Dan Feldman
 */
@XmlType(name = "CocoaPodsConfigurationType", propOrder = {"cocoaPodsSpecsRepoUrl", "specRepoProvider"},
        namespace = Descriptor.NS)
public class CocoaPodsConfiguration implements Descriptor {

    @XmlElement(defaultValue = "https://github.com/CocoaPods/Specs", required = false)
    private String cocoaPodsSpecsRepoUrl = "https://github.com/CocoaPods/Specs";

    @XmlElement(name = "specRepoProvider")
    private VcsGitConfiguration specRepoProvider = new VcsGitConfiguration();

    public String getCocoaPodsSpecsRepoUrl() {
        return cocoaPodsSpecsRepoUrl;
    }

    public void setCocoaPodsSpecsRepoUrl(String cocoaPodsSpecsRepoUrl) {
        this.cocoaPodsSpecsRepoUrl = cocoaPodsSpecsRepoUrl;
    }

    public VcsGitConfiguration getSpecRepoProvider() {
        return specRepoProvider;
    }

    public void setSpecRepoProvider(VcsGitConfiguration specRepoProvider) {
        this.specRepoProvider = specRepoProvider;
    }
}
