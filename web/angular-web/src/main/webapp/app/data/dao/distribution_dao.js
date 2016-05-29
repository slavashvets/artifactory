export function DistributionDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setPath(RESOURCE.BINTRAY_DISTRIBUTION + "/:action")
        .setCustomActions({
            'distributeArtifact': {
                method: 'POST',
                notifications: true,
                params: {action: 'distributeArtifact'}
            },
            'distributeBuild': {
                method: 'POST',
                notifications: true,
                params: {action: 'distributeBuild'}
            },
            'getAvailableDistributionRepos': {
                method: 'GET',
                params: {action: 'getAvailableDistributionRepos'},
                isArray: true
            }
        })
        .getInstance();
}