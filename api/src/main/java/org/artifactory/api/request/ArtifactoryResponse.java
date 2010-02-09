/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.request;

import org.artifactory.api.common.StatusHolder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

public interface ArtifactoryResponse {

    void setException(Exception exception);

    enum Status {
        UNSET, SUCCESS, FAILURE
    }

    void setLastModified(long lastModified);

    void setEtag(String etag);

    void setContentLength(int length);

    int getContentLength();

    boolean isContentLengthSet();

    void setContentType(String contentType);

    OutputStream getOutputStream() throws IOException;

    Writer getWriter() throws IOException;

    void sendInternalError(Exception exception, Logger logger) throws IOException;

    void sendError(int statusCode, String reason, Logger logger) throws IOException;

    void sendError(StatusHolder statusHolder) throws IOException;

    void sendStream(InputStream is) throws IOException;

    void sendFile(File targetFile) throws IOException;

    void sendOk();

    void setStatus(int status);

    void setHeader(String header, String value);

    boolean isCommitted();

    boolean isSuccessful();

    void flush();

    Exception getException();

    void sendAuthorizationRequired(String message, String realm) throws IOException;
}