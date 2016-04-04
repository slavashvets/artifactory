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

import java.util.List;
import java.util.Set;

import org.artifactory.storage.binstore.config.model.Property;
import org.artifactory.storage.binstore.config.model.ProviderMetaData;
import org.artifactory.storage.binstore.ifc.model.BinaryTreeElement;

import com.google.common.collect.Lists;

/**
 * Date: 12/12/12
 * Time: 3:03 PM
 *
 * @author freds
 */
public abstract class BinaryProviderBase implements BinaryProvider {
    MutableBinaryProvider mutableBinaryProvider;

    public void initialize() {
        mutableBinaryProvider.initialize();
    }

    public BinaryProviderBase next() {
        return mutableBinaryProvider.next();
    }

    public long getLongParam(String name) {
        return mutableBinaryProvider.getLongParam(name);
    }

    public int getIntParam(String name) {
        return mutableBinaryProvider.getIntParam(name);
    }

    public boolean getBooleanParam(String name) {
        return mutableBinaryProvider.getBooleanParam(name);
    }

    public Set<Property> getProperties() {
        return mutableBinaryProvider.getproperties();
    }

    public BinaryProviderServices getBinaryStoreServices() {
        return mutableBinaryProvider.getBinaryProviderServices();
    }

    public ProviderMetaData getProviderMetaData() {
        return mutableBinaryProvider.getProviderMetaData();
    }

    public List<BinaryProviderBase> getSubBinaryProviders() {
        return mutableBinaryProvider.getSubBinaryProviders();
    }

    public BinaryProviderBase getBinaryProvider() {
        return mutableBinaryProvider.getBinaryProvider();
    }

    public String getParam(String name) {
        return mutableBinaryProvider.getParam(name);
    }

    public String getProperty(String name) {
        return mutableBinaryProvider.getProperty(name);
    }

    public void collect(BinaryProviderCollector binaryProviderCollector) {
        binaryProviderCollector.collect(this);
        if (next() != null) {
            next().collect(binaryProviderCollector);
        }
        for (BinaryProviderBase providerBase : getSubBinaryProviders()) {
            providerBase.collect(binaryProviderCollector);
        }
    }

    public <T> BinaryTreeElement<T> visit(BinaryProviderVisitor<T> binaryProviderVisitor) {
        BinaryTreeElement<T> element = new BinaryTreeElement<>();
        T data = binaryProviderVisitor.visit(this);
        element.setData(data);
        if (next() != null) {
            element.setNextBinaryTreeElement(next().visit(binaryProviderVisitor));
        }
        List<BinaryTreeElement<T>> list = Lists.newArrayList();
        element.setSubBinaryTreeElements(list);
        for (BinaryProviderBase providerBase : getSubBinaryProviders()) {
            list.add(providerBase.visit(binaryProviderVisitor));
        }
        return element;

    }
}
