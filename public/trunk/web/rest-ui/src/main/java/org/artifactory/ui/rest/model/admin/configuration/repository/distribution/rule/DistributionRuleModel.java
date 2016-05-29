package org.artifactory.ui.rest.model.admin.configuration.repository.distribution.rule;

import org.artifactory.api.bintray.distribution.rule.DistributionRuleTokens;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
public class DistributionRuleModel implements RestModel {

    private String name;
    private String packageType;
    private String repoFilter;
    private String pathFilter;
    private DistributionCoordinatesModel distributionCoordinates;
    private List<DistributionRuleTokenModel> availableTokens;

    public DistributionRuleModel() {

    }

    public DistributionRuleModel(DistributionRule distRule) {
        this.name = distRule.getName();
        this.packageType = distRule.getType().name();
        this.repoFilter = distRule.getRepoFilter();
        this.pathFilter = distRule.getPathFilter();
        this.distributionCoordinates = new DistributionCoordinatesModel(distRule.getDistributionCoordinates());
        this.availableTokens = DistributionRuleTokens.tokensByType(distRule.getType())
                .stream()
                .map(DistributionRuleTokenModel::new)
                .collect(Collectors.toList());
        this.availableTokens.add(new DistributionRuleTokenModel(DistributionRuleTokens.getProductNameToken()));
    }

    public static DistributionRule ruleFromModel(DistributionRuleModel model) {
        DistributionRule rule = new DistributionRule();
        rule.setName(model.name);
        rule.setType(RepoType.fromType(model.packageType));
        rule.setRepoFilter(model.repoFilter);
        rule.setPathFilter(model.pathFilter);
        rule.setDistributionCoordinates(model.distributionCoordinates.toCoordinates());
        return rule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getRepoFilter() {
        return repoFilter;
    }

    public void setRepoFilter(String repoFilter) {
        this.repoFilter = repoFilter;
    }

    public String getPathFilter() {
        return pathFilter;
    }

    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }

    public DistributionCoordinatesModel getDistributionCoordinates() {
        return distributionCoordinates;
    }

    public void setDistributionCoordinates(DistributionCoordinatesModel distributionCoordinates) {
        this.distributionCoordinates = distributionCoordinates;
    }

    public List<DistributionRuleTokenModel> getAvailableTokens() {
        return availableTokens;
    }

    public void setAvailableTokens(List<DistributionRuleTokenModel> availableTokens) {
        this.availableTokens = availableTokens;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
