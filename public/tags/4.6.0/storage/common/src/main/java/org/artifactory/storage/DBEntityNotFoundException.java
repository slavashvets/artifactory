package org.artifactory.storage;

import org.artifactory.storage.binstore.exceptions.StorageException;

/**
 * Exception for DB entities which are not found
 * @author Shay Bagants
 */
public class DBEntityNotFoundException extends StorageException {
    public DBEntityNotFoundException(String message) {
        super(message);
    }

    public DBEntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBEntityNotFoundException(Throwable cause) {
        super(cause);
    }
}
