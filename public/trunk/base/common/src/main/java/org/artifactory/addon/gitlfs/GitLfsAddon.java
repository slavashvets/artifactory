package org.artifactory.addon.gitlfs;

import org.apache.sshd.server.Command;
import org.artifactory.addon.Addon;
import org.artifactory.security.props.auth.SshTokenManager;


/**
 * @author Chen Keinan
 */
public interface GitLfsAddon extends Addon {

    boolean isGitLfsCommand(String command);

    Command createGitLfsCommand(String command, SshTokenManager sshTokenManager);

}
