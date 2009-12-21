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

package org.artifactory.common.wicket.panel.editor;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;

/**
 * Created by IntelliJ IDEA. User: noam
 */
public class TextEditorPanel extends TitledPanel {

    private TextArea editorTextArea;
    private String title;

    @WicketProperty
    private String editorValue;

    public TextEditorPanel(String id, String title, String helpMessage) {
        this(id, title, new Model(helpMessage));
    }

    public TextEditorPanel(String id, String title, IModel helpModel) {
        super(id, helpModel);
        this.title = title;
        addTextArea();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    protected Component newToolbar(String id) {
        return new HelpBubble(id, getModel());
    }

    public void setEditorValue(String pomText) {
        editorValue = pomText;
    }

    public String getEditorValue() {
        return editorValue;
    }

    public void addTextAreaBehavior(AjaxFormComponentUpdatingBehavior behavior) {
        editorTextArea.add(behavior);
    }

    private void addTextArea() {
        editorTextArea = new TextArea("editorTextArea", newTextModel());
        add(editorTextArea);
    }

    protected IModel newTextModel() {
        return new PropertyModel(this, "editorValue");
    }
}
