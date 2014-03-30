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

package org.artifactory.repo.webdav;

import org.apache.commons.httpclient.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps the HttpServletResponse class to abstract the specific protocol used. To support other protocols we would only
 * need to modify this class.
 *
 * @author Marc Eaddy
 * @version $Revision$, $Date$
 */
public class WebdavStatus {

    // ------------------------------------------------------ HTTP Status Codes

    /**
     * Status code (200) indicating the request succeeded normally.
     */
    public static final int SC_OK = HttpStatus.SC_OK;

    /**
     * Status code (201) indicating the request succeeded and created a new resource on the server.
     */
    public static final int SC_CREATED = HttpStatus.SC_CREATED;

    /**
     * Status code (202) indicating that a request was accepted for processing, but was not completed.
     */
    public static final int SC_ACCEPTED = HttpStatus.SC_ACCEPTED;

    /**
     * Status code (204) indicating that the request succeeded but that there was no new information to return.
     */
    public static final int SC_NO_CONTENT = HttpStatus.SC_NO_CONTENT;

    /**
     * Status code (301) indicating that the resource has permanently moved to a new location, and that future
     * references should use a new URI with their requests.
     */
    public static final int SC_MOVED_PERMANENTLY = HttpStatus.SC_MOVED_PERMANENTLY;

    /**
     * Status code (302) indicating that the resource has temporarily moved to another location, but that future
     * references should still use the original URI to access the resource.
     */
    public static final int SC_MOVED_TEMPORARILY = HttpStatus.SC_MOVED_TEMPORARILY;

    /**
     * Status code (304) indicating that a conditional GET operation found that the resource was available and not
     * modified.
     */
    public static final int SC_NOT_MODIFIED = HttpStatus.SC_NOT_MODIFIED;

    /**
     * Status code (400) indicating the request sent by the client was syntactically incorrect.
     */
    public static final int SC_BAD_REQUEST = HttpStatus.SC_BAD_REQUEST;

    /**
     * Status code (401) indicating that the request requires HTTP authentication.
     */
    public static final int SC_UNAUTHORIZED = HttpStatus.SC_UNAUTHORIZED;

    /**
     * Status code (403) indicating the server understood the request but refused to fulfill it.
     */
    public static final int SC_FORBIDDEN = HttpStatus.SC_FORBIDDEN;

    /**
     * Status code (404) indicating that the requested resource is not available.
     */
    public static final int SC_NOT_FOUND = HttpStatus.SC_NOT_FOUND;

    /**
     * Status code (500) indicating an error inside the HTTP service which prevented it from fulfilling the request.
     */
    public static final int SC_INTERNAL_SERVER_ERROR = HttpStatus.SC_INTERNAL_SERVER_ERROR;

    /**
     * Status code (501) indicating the HTTP service does not support the functionality needed to fulfill the request.
     */
    public static final int SC_NOT_IMPLEMENTED = HttpStatus.SC_NOT_IMPLEMENTED;

    /**
     * Status code (502) indicating that the HTTP server received an invalid response from a server it consulted when
     * acting as a proxy or gateway.
     */
    public static final int SC_BAD_GATEWAY = HttpStatus.SC_BAD_GATEWAY;

    /**
     * Status code (503) indicating that the HTTP service is temporarily overloaded, and unable to handle the request.
     */
    public static final int SC_SERVICE_UNAVAILABLE = HttpStatus.SC_SERVICE_UNAVAILABLE;

    /**
     * Status code (100) indicating the client may continue with its request.  This interim response is used to inform
     * the client that the initial part of the request has been received and has not yet been rejected by the server.
     */
    public static final int SC_CONTINUE = HttpStatus.SC_CONTINUE;

    /**
     * Status code (405) indicating the method specified is not allowed for the resource.
     */
    public static final int SC_METHOD_NOT_ALLOWED = HttpStatus.SC_METHOD_NOT_ALLOWED;

    /**
     * Status code (409) indicating that the request could not be completed due to a conflict with the current state of
     * the resource.
     */
    public static final int SC_CONFLICT = HttpStatus.SC_CONFLICT;

    /**
     * Status code (412) indicating the precondition given in one or more of the request-header fields evaluated to
     * false when it was tested on the server.
     */
    public static final int SC_PRECONDITION_FAILED = HttpStatus.SC_PRECONDITION_FAILED;

    /**
     * Status code (413) indicating the server is refusing to process a request because the request entity is larger
     * than the server is willing or able to process.
     */
    public static final int SC_REQUEST_ENTITY_TOO_LARGE =
            413;

    /**
     * Status code (415) indicating the server is refusing to service the request because the entity of the request is
     * in a format not supported by the requested resource for the requested method.
     */
    public static final int SC_UNSUPPORTED_MEDIA_TYPE =
            HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE;

    // -------------------------------------------- Extended WebDav status code

    /**
     * Status code (207) indicating that the response requires providing status for multiple independent operations.
     */
    public static final int SC_MULTI_STATUS = 207;

    /**
     * Status code (422) indicating the entity body submitted with the PATCH method was not understood by the resource.
     */
    public static final int SC_UNPROCESSABLE_ENTITY = 422;

    /**
     * Status code (507) indicating that the resource does not have sufficient space to record the state of the resource
     * after the execution of this method.
     */
    public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 507;

    /**
     * Status code (424) indicating the method was not executed on a particular resource within its scope because some
     * part of the method's execution failed causing the entire method to be aborted.
     */
    public static final int SC_FAILED_DEPENDENCY = 424;

    /**
     * Status code (423) indicating the destination resource of a method is locked, and either the request did not
     * contain a valid Lock-Info header, or the Lock-Info header identifies a lock held by another principal.
     */
    public static final int SC_LOCKED = 423;

    /**
     * This Hashtable contains the mapping of HTTP and WebDAV status codes to descriptive text.
     */
    private static Map<Integer, String> mapStatusCodes = new HashMap<>();

    static {
        // HTTP 1.1 status codes
        addStatusCodeMap(SC_OK, "OK");
        addStatusCodeMap(SC_CREATED, "Created");
        addStatusCodeMap(SC_ACCEPTED, "Accepted");
        addStatusCodeMap(SC_NO_CONTENT, "No Content");
        addStatusCodeMap(SC_MOVED_PERMANENTLY, "Moved Permanently");
        addStatusCodeMap(SC_MOVED_TEMPORARILY, "Moved Temporarily");
        addStatusCodeMap(SC_NOT_MODIFIED, "Not Modified");
        addStatusCodeMap(SC_BAD_REQUEST, "Bad Request");
        addStatusCodeMap(SC_UNAUTHORIZED, "Unauthorized");
        addStatusCodeMap(SC_FORBIDDEN, "Forbidden");
        addStatusCodeMap(SC_NOT_FOUND, "Not Found");
        addStatusCodeMap(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        addStatusCodeMap(SC_NOT_IMPLEMENTED, "Not Implemented");
        addStatusCodeMap(SC_BAD_GATEWAY, "Bad Gateway");
        addStatusCodeMap(SC_SERVICE_UNAVAILABLE, "Service Unavailable");
        addStatusCodeMap(SC_CONTINUE, "Continue");
        addStatusCodeMap(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
        addStatusCodeMap(SC_CONFLICT, "Conflict");
        addStatusCodeMap(SC_PRECONDITION_FAILED, "Precondition Failed");
        addStatusCodeMap(SC_REQUEST_ENTITY_TOO_LARGE, "Request Too Long");
        addStatusCodeMap(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
        // WebDav status sodes
        addStatusCodeMap(SC_MULTI_STATUS, "Multi-Status");
        addStatusCodeMap(SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity");
        addStatusCodeMap(SC_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space On Resource");
        addStatusCodeMap(SC_FAILED_DEPENDENCY, "Failed Dependency");
        addStatusCodeMap(SC_LOCKED, "Locked");
    }

    /**
     * Returns the HTTP status text for the HTTP or WebDav status code specified by looking it up in the mapping.
     *
     * @param statusCode HTTP or WebDAV status code
     * @return A string with a short descriptive phrase for the HTTP status code (e.g., "OK").
     */
    public static String getStatusText(int statusCode) {

        if (!mapStatusCodes.containsKey(statusCode)) {
            return "";
        } else {
            return mapStatusCodes.get(statusCode);
        }
    }

    /**
     * Adds a new status code -> status text mapping.  This is a static method because the mapping is a static
     * variable.
     *
     * @param key    HTTP or WebDAV status code
     * @param strVal HTTP status text
     */
    private static void addStatusCodeMap(int key, String strVal) {
        mapStatusCodes.put(key, strVal);
    }
}