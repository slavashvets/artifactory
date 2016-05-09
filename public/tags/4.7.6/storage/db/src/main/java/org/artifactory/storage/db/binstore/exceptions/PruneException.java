package org.artifactory.storage.db.binstore.exceptions;

/**
 * @author gidis
 */
public class PruneException extends RuntimeException {
    public PruneException(String message) {
        super(message);
    }
}
