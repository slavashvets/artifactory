package org.artifactory.storage.db.binstore.visitors;

import com.google.common.collect.Lists;
import org.jfrog.storage.binstore.ifc.model.BinaryTreeElement;

import java.util.List;

/**
 * @author gidis
 */
public class BinaryTreeElementScanner<T, Y> {

    public BinaryTreeElement<Y> scan(BinaryTreeElement<T> binaryTreeElement, BinaryTreeElementHandler<T, Y> handler) {
        BinaryTreeElement<Y> element = new BinaryTreeElement<>();
        Y data = handler.visit(binaryTreeElement);
        if (data != null) {
            element.setData(data);
            if (binaryTreeElement.getNextBinaryTreeElement() != null) {
                element.setNextBinaryTreeElement(scan(binaryTreeElement.getNextBinaryTreeElement(), handler));
            }
            List<BinaryTreeElement<Y>> list = Lists.newArrayList();
            element.setSubBinaryTreeElements(list);
            for (BinaryTreeElement<T> subElement : binaryTreeElement.getSubBinaryTreeElements()) {
                list.add(scan(subElement, handler));
            }
            return element;
        } else {
            if (binaryTreeElement.getNextBinaryTreeElement() != null) {
                return scan(binaryTreeElement.getNextBinaryTreeElement(), handler);
            }
        }
        return null;
    }
}
