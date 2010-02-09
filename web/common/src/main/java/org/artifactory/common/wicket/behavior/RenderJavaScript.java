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

package org.artifactory.common.wicket.behavior;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.JavascriptUtils;

/**
 * @author Yoav Aharoni
 */
public class RenderJavaScript extends AbstractBehavior {
    private IModel javascript;

    public RenderJavaScript(String javascript) {
        this(new Model(javascript));
    }

    public RenderJavaScript(IModel javascript) {
        this.javascript = javascript;
    }

    public String getJavascript() {
        return javascript.getObject().toString();
    }

    @Override
    public void onRendered(Component component) {
        super.onRendered(component);
        final AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            target.appendJavascript(getJavascript());
        } else {
            final Response response = RequestCycle.get().getResponse();
            JavascriptUtils.writeJavascript(response, getJavascript());
        }
    }
}
