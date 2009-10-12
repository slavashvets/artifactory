/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.servlet;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.util.HttpUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ArtifactoryFilter implements Filter {

    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        try {
            bind();
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                if (!httpResponse.containsHeader("Server")) {
                    //Add the server header (curl -I http://localhost:8080/artifactory/)
                    httpResponse.setHeader("Server", HttpUtils.getArtifactoryUserAgent());
                }
            }
            chain.doFilter(request, response);
        } finally {
            unbind();
        }
    }

    private void bind() {
        ServletContext servletContext = filterConfig.getServletContext();
        ArtifactoryContext context = RequestUtils.getArtifactoryContext(servletContext);
        ArtifactoryContextThreadBinder.bind(context);
        ArtifactorySystemProperties.bind(context.getArtifactoryHome().getArtifactoryProperties());
    }

    private void unbind() {
        ArtifactoryContextThreadBinder.unbind();
        ArtifactorySystemProperties.unbind();
    }

    public void destroy() {
    }
}