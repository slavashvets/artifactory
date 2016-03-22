package org.artifactory.rest.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.exception.HaNodePropagationException;

/**
 * @author Shay Yaakov
 */
public class HaRestFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        HaCommonAddon haAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaCommonAddon.class);
        if (haAddon.isHaEnabled()) {
            String nodeId = containerRequest.getHeaderValue(HaCommonAddon.ARTIFACTORY_NODE_ID);
            if (StringUtils.isNotBlank(nodeId) && StringUtils.contains(containerRequest.getRequestUri().getPath(), "/mc/")) {
                if (!StringUtils.equals(haAddon.getCurrentMemberServerId(), nodeId)) {
                    throw new HaNodePropagationException(containerRequest, nodeId);
                }
            }
        }
        return containerRequest;
    }
}
