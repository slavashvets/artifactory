package org.artifactory.security.props.auth.model;

/**
 * @author Chen Keinan
 */
public class OauthDockerErrorModel implements OauthModel {

    private int statusCode;
    private OauthErrorEnum details;

    public OauthDockerErrorModel() {
    }

    public OauthDockerErrorModel(int statusCode, OauthErrorEnum internalErrorMsg) {
        this.statusCode = statusCode;
        this.details = internalErrorMsg;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public OauthErrorEnum getDetails() {
        return details;
    }

    public void setDetails(OauthErrorEnum details) {
        this.details = details;
    }
}
