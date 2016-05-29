package org.artifactory.descriptor.repo.distribution.rule;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Shay Yaakov
 */
@Test
public class DefaultDistributionRulesTest {

    public void testDefaultRules() throws Exception {
        List<DistributionRule> defaultRules = DefaultDistributionRules.getDefaultRules();
        assertEquals(defaultRules.size(), 13);
        assertEquals(defaultRules.get(0).name, "Bower-default");
        assertEquals(defaultRules.get(1).name, "CocoaPods-default");
        assertEquals(defaultRules.get(2).name, "Debian-default");
        assertEquals(defaultRules.get(3).name, "Docker-default");
        assertEquals(defaultRules.get(4).name, "Gradle-default");
        assertEquals(defaultRules.get(5).name, "Ivy-default");
        assertEquals(defaultRules.get(6).name, "Maven-default");
        assertEquals(defaultRules.get(7).name, "Npm-default");
        assertEquals(defaultRules.get(8).name, "NuGet-default");
        assertEquals(defaultRules.get(9).name, "Opkg-default");
        assertEquals(defaultRules.get(10).name, "Yum-default");
        assertEquals(defaultRules.get(11).name, "Sbt-default");
        assertEquals(defaultRules.get(12).name, "Vagrant-default");
    }

    public void testDefaultProductRules() throws Exception {
        List<DistributionRule> defaultProductRules = DefaultDistributionRules.getDefaultProductRules();
        assertEquals(defaultProductRules.size(), 13);
        assertEquals(defaultProductRules.get(0).name, "Bower-product-default");
        assertEquals(defaultProductRules.get(1).name, "CocoaPods-product-default");
        assertEquals(defaultProductRules.get(2).name, "Debian-product-default");
        assertEquals(defaultProductRules.get(3).name, "Docker-product-default");
        assertEquals(defaultProductRules.get(4).name, "Gradle-product-default");
        assertEquals(defaultProductRules.get(5).name, "Ivy-product-default");
        assertEquals(defaultProductRules.get(6).name, "Maven-product-default");
        assertEquals(defaultProductRules.get(7).name, "Npm-product-default");
        assertEquals(defaultProductRules.get(8).name, "NuGet-product-default");
        assertEquals(defaultProductRules.get(9).name, "Opkg-product-default");
        assertEquals(defaultProductRules.get(10).name, "Yum-product-default");
        assertEquals(defaultProductRules.get(11).name, "Sbt-product-default");
        assertEquals(defaultProductRules.get(12).name, "Vagrant-product-default");
    }
}