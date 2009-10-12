/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.security.acl;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class PermissionTargetListPanel extends ModalListPanel<PermissionTargetInfo> {
    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private AclService security;

    public PermissionTargetListPanel(String id) {
        super(id);

        if (!authService.isAdmin()) {
            disableNewItemLink();
        }

        getDataProvider().setSort("name", true);
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    protected List<PermissionTargetInfo> getList() {
        return security.getPermissionTargets(ArtifactoryPermission.ADMIN);
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Permission Target Name"), "name", "name"));
        columns.add(new PropertyColumn(new Model("Repositories"), "repoKeys", "repoKeys"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new PermissionTargetCreateUpdatePanel(
                CreateUpdateAction.CREATE,
                new PermissionTargetInfo(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(PermissionTargetInfo permissionTarget) {
        return new PermissionTargetCreateUpdatePanel(
                CreateUpdateAction.UPDATE,
                permissionTarget, this);
    }

    @Override
    protected String getDeleteConfirmationText(PermissionTargetInfo permissionTarget) {
        return "Are you sure you wish to delete the target " +
                permissionTarget.getName() + "?";
    }

    @Override
    protected void deleteItem(PermissionTargetInfo permissionTarget, AjaxRequestTarget target) {
        security.deleteAcl(permissionTarget);
    }

}
