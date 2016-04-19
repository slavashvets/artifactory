import {ArtifactoryDao} from '../artifactory_dao';

export function GlobalReplicationsConfigDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setPath(RESOURCE.GLOBAL_REPLICATIONS_BLOCK)
        .setCustomActions({
            'status': {
                method: 'GET'
            }
        })
        .getInstance();
}