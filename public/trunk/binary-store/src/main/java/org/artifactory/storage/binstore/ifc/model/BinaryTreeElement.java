package org.artifactory.storage.binstore.ifc.model;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Gidi Shabat
 */
public class BinaryTreeElement<T> implements Serializable {
    private T data;
    private BinaryTreeElement<T> nextBinaryTreeElement;
    private List<BinaryTreeElement<T>> subBinaryTreeElements = Lists.newArrayList();

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public BinaryTreeElement<T> getNextBinaryTreeElement() {
        return nextBinaryTreeElement;
    }

    public void setNextBinaryTreeElement(BinaryTreeElement<T> next) {
        this.nextBinaryTreeElement = next;
    }

    public List<BinaryTreeElement<T>> getSubBinaryTreeElements() {
        return subBinaryTreeElements;
    }

    public void setSubBinaryTreeElements(List<BinaryTreeElement<T>> subBinaryTreeElements) {
        this.subBinaryTreeElements = subBinaryTreeElements;
    }
}
