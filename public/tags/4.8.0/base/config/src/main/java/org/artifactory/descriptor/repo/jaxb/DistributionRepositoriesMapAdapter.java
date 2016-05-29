package org.artifactory.descriptor.repo.jaxb;

import com.google.common.collect.Maps;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.util.PathUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
public class DistributionRepositoriesMapAdapter
        extends XmlAdapter<DistributionRepositoriesMapAdapter.Wrapper, Map<String, DistributionRepoDescriptor>> {

    @Override
    public Map<String, DistributionRepoDescriptor> unmarshal(Wrapper wrapper) throws Exception {
        Map<String, DistributionRepoDescriptor> distributionRepositoriesMap = Maps.newLinkedHashMap();
        List<String> duplicateRepos = wrapper.getList().stream()
                .map(repo -> distributionRepositoriesMap.put(repo.getKey(), repo))
                .filter(duplicateRepo -> duplicateRepo != null)
                .map(DistributionRepoDescriptor::getKey)
                .collect(Collectors.toList());

        if (!duplicateRepos.isEmpty()) {
            //Throw an error since jaxb swallows exceptions
            throw new Error("Duplicate repository keys in configuration: "
                            + PathUtils.collectionToDelimitedString(duplicateRepos) + ".");
        }
        return distributionRepositoriesMap;
    }

    @Override
    public Wrapper marshal(Map<String, DistributionRepoDescriptor> map) throws Exception {
        return new Wrapper(map);
    }

    @XmlType(name = "DistributionRepositoriesType", namespace = Descriptor.NS)
    public static class Wrapper {
        @XmlElement(name = "distributionRepository", required = true, namespace = Descriptor.NS)
        private List<DistributionRepoDescriptor> list = new ArrayList<>();

        public Wrapper() {
        }

        public Wrapper(Map<String, DistributionRepoDescriptor> map) {
            list.addAll(map.values().stream().collect(Collectors.toList()));
        }

        public List<DistributionRepoDescriptor> getList() {
            return list;
        }
    }
}
