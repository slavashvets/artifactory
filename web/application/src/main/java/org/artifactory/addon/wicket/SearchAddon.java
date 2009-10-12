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

package org.artifactory.addon.wicket;

import org.apache.wicket.model.IModel;
import org.artifactory.addon.AddonFactory;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.webapp.wicket.page.search.LimitlessCapableSearcher;
import org.artifactory.webapp.wicket.page.search.SaveSearchResultsPanel;

/**
 * Search features addon.
 *
 * @author Yossi Shaul
 */
public interface SearchAddon extends AddonFactory {
    /**
     * @return Panel to manage search results.
     */
    SaveSearchResultsPanel getSaveSearchResultsPanel(String wicketId, IModel model,
            LimitlessCapableSearcher limitlessCapableSearcher);

    MenuNode getBrowserSearchMenuNode();
}