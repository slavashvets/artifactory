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

package org.artifactory.ui.rest.service.utils.validation;

import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.exception.ValidationException;

/**
 * Checks if an xml id is unique in the central config descriptor.
 *
 * @author Yossi Shaul
 */
public final class UniqueXmlIdValidator {
    private MutableCentralConfigDescriptor centralConfig;

    public UniqueXmlIdValidator(MutableCentralConfigDescriptor centralConfig) {
        this.centralConfig = centralConfig;
    }

    public void validate(String id) throws ValidationException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("ID cannot be empty");
        }
        if (!centralConfig.isKeyAvailable(id)) {
            throw new ValidationException(String.format("The ID '%s' is already used.", id));
        }
    }
}