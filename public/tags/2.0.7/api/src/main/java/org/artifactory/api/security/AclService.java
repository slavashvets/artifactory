/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.security;

import org.artifactory.api.repo.Lock;

import java.util.List;

/**
 * User: freds Date: Aug 5, 2008 Time: 8:46:40 PM
 */
public interface AclService {
    /**
     * @return A List of all the permission targets the current authenticated user can administer.
     */
    List<PermissionTargetInfo> getAdministrativePermissionTargets();

    /**
     * @return A List of all the permission targets the current authenticated user can deploy to.
     */
    List<PermissionTargetInfo> getDeployablePermissionTargets();

    boolean canAdmin(PermissionTargetInfo target);

    @Lock(transactional = true)
    AclInfo createAcl(PermissionTargetInfo entity);

    @Lock(transactional = true)
    void deleteAcl(PermissionTargetInfo target);

    AclInfo updateAcl(PermissionTargetInfo target);

    AclInfo getAcl(PermissionTargetInfo permissionTarget);

    void updateAcl(AclInfo acl);
}
