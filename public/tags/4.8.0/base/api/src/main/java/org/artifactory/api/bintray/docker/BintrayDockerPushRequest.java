package org.artifactory.api.bintray.docker;

/**
 * @author Shay Yaakov
 */
public class BintrayDockerPushRequest {

    public String dockerRepository;
    public String dockerTagName;
    public String bintraySubject;
    public String bintrayRepo;
    public String bintrayPackage;
    public String bintrayTag;
    public boolean async;
}
