package org.artifactory.storage.db.binstore.visitors;

import org.artifactory.storage.binstore.ifc.model.BinaryTreeElement;

/**
 * @author gidis
 */
public interface BinaryTreeElementHandler<T, Y> {
    Y visit(BinaryTreeElement<T> binaryTreeElement);
}
