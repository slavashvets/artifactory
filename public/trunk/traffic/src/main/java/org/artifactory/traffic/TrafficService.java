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

package org.artifactory.traffic;

import org.artifactory.traffic.entry.TrafficEntry;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;

/**
 * The main inteface of the traffic service
 *
 * @author Noam Tenne
 */
public interface TrafficService {
    /**
     * Get a list of traffic entries for the specified time window (edges inclusive)
     */
    @Transactional
    List<TrafficEntry> getEntryList(Calendar from, Calendar to);

    /**
     * Store a new traffic entry for later processing (collection) dd
     *
     * @param enrty
     */
    void handleTrafficEntry(TrafficEntry enrty);
}