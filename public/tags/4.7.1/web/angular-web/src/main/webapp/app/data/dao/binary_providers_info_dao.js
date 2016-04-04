import {ArtifactoryDao} from '../artifactory_dao';

export function BinaryProvidersInfoDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.BINARY_PROVIDERS_INFO)
            .setCustomActions({
                'get':{
                    method: 'GET'
                }
            });
}