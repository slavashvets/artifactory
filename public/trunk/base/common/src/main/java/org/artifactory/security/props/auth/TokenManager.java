package org.artifactory.security.props.auth;


import org.artifactory.security.props.auth.model.TokenKeyValue;

/**
 * @author Chen Keinan
 */
public interface TokenManager {

    /**
     * generate a token, does <b><i>not</i></b> store it
     *
     * @param userName - userName to create token for
     * @return Token data
     */
    TokenKeyValue generateToken(String userName);

    /**
     * create token (user props) and store it in DB
     *
     * @param userName - userName to create token for
     * @return Token data
     */
    TokenKeyValue createToken(String userName);

    /**
     * delete current token and create  new token (user props) and store it in DB
     *
     * @param userName - userName to refresh token for
     * @return Token data
     */
    TokenKeyValue refreshToken(String userName);

    /**
     * delete current token and store new given token
     *
     * @param userName - userName to update token for
     * @param token - new token to store
     * @return Token data
     */
    TokenKeyValue updateToken(String userName, String token);

    /**
     * get current token
     *
     * @param userName - userName to get token for
     * @return Token data
     */
    TokenKeyValue getToken(String userName);

    /**
     * delete current token from DB
     *
     * @param userName - userName to delete token for
     * @return Token data
     */
    boolean revokeToken(String userName);

    /**
     * delete all tokens in user props table DB
     *
     * @return Token data
     */
    boolean revokeAllTokens();

    /**
     * add external Token (git enterprise and etc) to db
     *
     * @param userName
     * @param extToken
     * @return
     */
    TokenKeyValue addExternalToken(String userName, String extToken);

}
