package org.artifactory.descriptor.repo.jaxb;

import com.google.common.collect.Maps;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.BintrayApplicationConfig;
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
public class BintrayApplicationConfigsMapAdapter
        extends XmlAdapter<BintrayApplicationConfigsMapAdapter.Wrapper, Map<String, BintrayApplicationConfig>> {

    @Override
    public Map<String, BintrayApplicationConfig> unmarshal(Wrapper wrapper) throws Exception {
        Map<String, BintrayApplicationConfig> bintrayAppConfigsMap = Maps.newLinkedHashMap();
        List<String> duplicateRepos = wrapper.getList().stream()
                .map(appConfig -> bintrayAppConfigsMap.put(appConfig.getKey(), appConfig))
                .filter(duplicateAppConfig -> duplicateAppConfig != null)
                .map(BintrayApplicationConfig::getKey)
                .collect(Collectors.toList());

        if (!duplicateRepos.isEmpty()) {
            //Throw an error since jaxb swallows exceptions
            throw new Error("Duplicate Bintray OAUth Application in configuration: "
                    + PathUtils.collectionToDelimitedString(duplicateRepos) + ".");
        }
        return bintrayAppConfigsMap;
    }

    @Override
    public Wrapper marshal(Map<String, BintrayApplicationConfig> map) throws Exception {
        return new Wrapper(map);
    }

    @XmlType(name = "BintrayApplicationsType", namespace = Descriptor.NS)
    public static class Wrapper {
        @XmlElement(name = "bintrayApplication", required = true, namespace = Descriptor.NS)
        private List<BintrayApplicationConfig> list = new ArrayList<>();

        public Wrapper() {
        }

        public Wrapper(Map<String, BintrayApplicationConfig> map) {
            list.addAll(map.values().stream().collect(Collectors.toList()));
        }

        public List<BintrayApplicationConfig> getList() {
            return list;
        }
    }
}
