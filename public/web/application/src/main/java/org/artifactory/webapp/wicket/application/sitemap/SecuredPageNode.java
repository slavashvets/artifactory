/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.model.sitemap.MenuNode;

/**
 * @author Yoav Aharoni
 */
public abstract class SecuredPageNode extends MenuNode {
    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repositoryService;

    protected SecuredPageNode(Class<? extends Page> pageClass, String name) {
        super(name, pageClass);
    }

    {
        InjectorHolder.getInjector().inject(this);
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public abstract boolean isEnabled();
}
