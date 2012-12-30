/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.logging.mbean;

import java.util.List;

/**
 * Error MBean main interface
 *
 * @author Noam Y. Tenne
 */
public interface ErrorMBean {

    /**
     * Returns the set of recorded errors
     *
     * @return Recorded errors by the time they occurred
     */
    List<String> getErrors();

    /**
     * Records an error
     *
     * @param newError Error to record
     */
    void addError(String newError);

    /**
     * Clears the error set
     */
    void clear();
}
