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

package org.artifactory.storage.binstore.binary.providers.base;

import com.google.common.collect.Lists;
import org.artifactory.storage.binstore.config.model.Property;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;

import java.util.List;
import java.util.Set;

/**
 * @author Gidi Shabat
 */
public class MutableBinaryProvider{
    // TODO: Refactor to a smaller interface neccessary for the binary provider
    // The goal is to limit access from Binary Provider to BinaryStore to the minimum needed
    private ProviderMetaData providerMetaData;
    private BinaryProviderBase binaryProvider;
    private BinaryProviderBase parentBinaryProvider;
    private List<BinaryProviderBase> subBinaryProviders = Lists.newArrayList();
    private BinaryProviderBase empty;
    private BinaryProviderServices binaryProviderServices;

    public void initialize() {
    }

    public BinaryProviderBase next() {
        if (binaryProvider == null) {
            return empty;
        }
        return binaryProvider;
    }

    public List<BinaryProviderBase> getSubBinaryProviders() {
        return subBinaryProviders;
    }

    public ProviderMetaData getProviderMetaData() {
        return providerMetaData;
    }

    public void setProviderMetaData(ProviderMetaData providerMetaData) {
        this.providerMetaData = providerMetaData;
    }

    public BinaryProviderBase getBinaryProvider() {
        return binaryProvider;
    }

    public void setBinaryProvider(BinaryProviderBase binaryProviderBase) {
        this.binaryProvider = binaryProviderBase;
    }

    public BinaryProviderServices getBinaryProviderServices() {
        return binaryProviderServices;
    }

    public void setBinaryProviderServices(BinaryProviderServices binaryProviderServices) {
        this.binaryProviderServices = binaryProviderServices;
    }

    public Set<Property> getproperties() {
        return providerMetaData.getProperties();
    }

    public String getProperty(String name) {
        return providerMetaData.getProperty(name);
    }

    public int getIntParam(String name) {
        String param = providerMetaData.getParamValue(name);
        return Integer.valueOf(param);
    }

    public boolean getBooleanParam(String name) {
        String param = providerMetaData.getParamValue(name);
        return Boolean.valueOf(param);
    }

    public long getLongParam(String name) {
        String param = providerMetaData.getParamValue(name);
        return Long.valueOf(param);
    }

    public String getParam(String name) {
        String param = providerMetaData.getParamValue(name);
        return param;
    }

    public BinaryProviderBase getParentBinaryProvider() {
        return parentBinaryProvider;
    }

    public void setParentBinaryProvider(BinaryProviderBase parentBinaryProvider) {
        this.parentBinaryProvider = parentBinaryProvider;
    }

    public void addSubBinaryProvider(BinaryProviderBase binaryProviderBase) {
        subBinaryProviders.add(binaryProviderBase);
    }

    public void setEmpty(BinaryProviderBase empty) {
        this.empty = empty;
    }
}
