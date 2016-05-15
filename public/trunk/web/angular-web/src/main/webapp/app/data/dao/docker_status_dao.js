import {ArtifactoryDao} from '../artifactory_dao';

export function DockerStatusDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.DOCKER_STATUS)
            .setCustomActions({
                'get':{
                    method: 'GET'
                }
            });
}