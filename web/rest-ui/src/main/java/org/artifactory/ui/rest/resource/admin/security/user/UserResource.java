package org.artifactory.ui.rest.resource.admin.security.user;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.model.admin.security.user.DeleteUsersModel;
import org.artifactory.ui.rest.model.admin.security.user.User;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@Path("users")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserResource extends BaseResource {

    @Autowired
    protected SecurityServiceFactory securityFactory;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(User userModel)
            throws Exception {
        return runService(securityFactory.createUser(), userModel);
    }

    @POST
    @Path("{userName}/expirePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response expireUserPassword(@PathParam("userName") String userName)
            throws Exception {
        return runService(securityFactory.expireUserPassword(), userName);
    }

    @POST
    @Path("{userName}/unexpirePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response unexpirePassword(@PathParam("userName") String userName)
            throws Exception {
        return runService(securityFactory.unexpirePassword(), userName);
    }

    @POST
    @Path("expirePasswordForAllUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response expirePasswordForAllUsers()
            throws Exception {
        return runService(securityFactory.expirePasswordForAllUsers());
    }

    @POST
    @Path("unexpirePasswordForAllUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response unexpirePasswordForAllUsers()
            throws Exception {
        return runService(securityFactory.unexpirePasswordForAllUsers());
    }

    @PUT
    @Path("{id : [^/]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(User userModel)
            throws Exception {
        return runService(securityFactory.updateUser(), userModel);
    }

    @POST
    @Path("userDelete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUsers(DeleteUsersModel deleteUsersModel) throws Exception {
        return runService(securityFactory.deleteUser(), deleteUsersModel);
    }

    @GET
    @Path("crud{id : (/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser()
            throws Exception {
        return runService(securityFactory.getUsers());
    }

    @GET
    @Path("permissions{id : /[^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserPermissions()
            throws Exception {
        return runService(securityFactory.getUserPermissions());
    }

    @POST
    @Path("externalStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkExternalStatus(User user)
            throws Exception {
        return runService(securityFactory.checkExternalStatus(), user);
    }
}
