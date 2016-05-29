package org.artifactory.security.props.auth;

import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * <p>Created on 04/05/16
 *
 * @author Yinon Avraham
 */
public interface PropsTokenCache {

    void put(TokenKeyValue tokenKeyValue, UserDetails principal);

    UserDetails get(TokenKeyValue tokenKeyValue);

}
