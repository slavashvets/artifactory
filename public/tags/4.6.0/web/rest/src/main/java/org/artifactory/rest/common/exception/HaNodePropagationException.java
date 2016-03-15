/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.rest.common.exception;

import com.sun.jersey.spi.container.ContainerRequest;

/**
 * @author Shay Yaakov
 */
public class HaNodePropagationException extends RuntimeException {

    private ContainerRequest request;
    private String nodeId;

    public HaNodePropagationException(ContainerRequest request, String nodeId) {
        this.request = request;
        this.nodeId = nodeId;
    }

    public ContainerRequest getRequest() {
        return request;
    }

    public String getNodeId() {
        return nodeId;
    }
}
