package org.artifactory.rest.resource.aql;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.AqlParserException;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.result.AqlQueryStreamResultIfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(AqlResource.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class AqlResource {
    private static final Logger log = LoggerFactory.getLogger(AqlResource.class);
    public static final String PATH_ROOT = "aql";
    @Context
    private HttpServletRequest request;
    @Autowired
    private AqlService aqlService;

    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public Response getLatestVersionByPath(String contentQuery) {
        //Try to load the query from URL params or attached file
        String query = getQuery(contentQuery);
        if (StringUtils.isBlank(query)) {
            log.error("Couldn't find the query neither in the request URL and the attached file");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        // Try to execute the query
        try {
            final AqlQueryStreamResultIfc aqlQueryStreamResult = aqlService.executeQueryLazy(query);
            // After success query execution prepare the result in stream.
            StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(OutputStream os) throws IOException, WebApplicationException {
                    byte value = (byte) aqlQueryStreamResult.read();
                    while (value >= 0) {
                        os.write(value);
                        value = (byte) aqlQueryStreamResult.read();
                    }
                    aqlQueryStreamResult.close();
                    //TODO How to ensure that we close the AQL Result
                }
            };
            return Response.ok(stream).build();
        } catch (AqlParserException e) {
            log.error("Fail to parse query: {}: ", query, e);
            if (e.getCause() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(e.getCause().getMessage()).build();
            }
        } catch (Exception e) {
            log.error("Fail to execute the following AqlApi, reason: ", e);
            return Response.serverError().build();
        }
    }

    private String getQuery(String contentQuery) {
        try {
            String query = contentQuery;
            if (StringUtils.isBlank(query)) {
                // Try to find query in the attached params ()
                query = (String) request.getParameterMap().get("query");
            }
            if (query != null) {
                query = URLDecoder.decode(query, "UTF-8");
            }
            return query;
        } catch (Exception e) {
            return null;
        }
    }
}
