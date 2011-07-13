/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.request;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

import java.io.IOException;
import java.io.InputStream;

public interface Request {

    RepoPath getRepoPath();

    boolean isChecksum();

    /**
     * Checks if the request originated from another artifactory
     *
     * @return
     */
    boolean isFromAnotherArtifactory();

    boolean isHeadOnly();

    long getLastModified();

    long getIfModifiedSince();

    boolean isNewerThan(long time);

    String getHeader(String headerName);

    String getServletContextUrl();

    String getUri();

    Properties getProperties();

    boolean hasProperties();

    public String getParameter(String name);

    public String[] getParameterValues(String name);

    InputStream getInputStream() throws IOException;

    /**
     * @return an integer containing the length in bytes of the request body or -1 if the length is not known
     */
    int getContentLength();

    /**
     * Get the address of the client that triggered the request.
     *
     * @return The client IP address as a string.
     */
    String getClientAddress();

    /**
     * Returns the internal zip resource path if such existed in the request.<p/>
     * For example if the request path is /path/to/zip!/path/to/resource/in/zip the method will return the zip resource
     * path: '/path/to/zip' as the root path.
     *
     * @return The zip resource path. Null or empty is such doesn't exist in the request path.
     */
    String getZipResourcePath();

    /**
     * @return True is the request is for internal zip resource
     * @see org.artifactory.request.Request#getZipResourcePath()
     */
    boolean isZipResourceRequest();
}
