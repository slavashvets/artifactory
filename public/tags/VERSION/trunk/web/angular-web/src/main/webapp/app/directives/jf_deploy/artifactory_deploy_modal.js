import EVENTS from '../../constants/artifacts_events.constants';
import API from '../../constants/api.constants';
import TOOLTIP from '../../constants/artifact_tooltip.constant';
import FIELD_OPTIONS from '../../constants/field_options.constats';

export class ArtifactoryDeployModal {

    constructor($rootScope, RepoDataDao, ArtifactoryEventBus, ArtifactoryModal,$timeout) {
        this.$rootScope = $rootScope;
        this.$timeout = $timeout;
        this.repoDataDao = RepoDataDao;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryModal = ArtifactoryModal;
        this.TOOLTIP = TOOLTIP.artifacts.deploy;
    }


    /**
     * Init the modal scope and launch the modal
     * @param node
     * @returns {modalInstance.result|*}
     */
    launch(node) {
        this.$scope = this.$rootScope.$new();
        this.$scope.Deploy = this;
        this.node = node;
        this._initDeploy();
        this.modalInstance = this.artifactoryModal.launchModal('deploy_modal', this.$scope, 600);
        return this.modalInstance.result;
    }

    /**
     * get repo list.
     * set controller (single or multi)
     * check if local repo
     * (set  local repo on comm obj)
     * @private
     */
    _initDeploy() {
        this.currentDeploy = 'Single';
        this.deployFile = {};
        this.comm = {};
        this.repoDataDao.get({deploy: 'true'}).$promise.then((result)=> {
            this.comm.reposList = result.repoTypesList;
            this.comm.reposList = _.map(this.comm.reposList,(repo)=>{
                if (repo.layoutPattern) {
                    repo.layoutPatternDisplay = repo.layoutPattern;

                    let pattern = repo.layoutPattern;
                    let nextToken = pattern.match(/\[(.*?)\]/);
                    while (nextToken) {
                        let safeHTML = nextToken[1].replace('<','&lt').replace('>','&gt');
                        repo.layoutPatternDisplay = repo.layoutPatternDisplay.split(nextToken[0]).join(`[${safeHTML}]`);
                        pattern = pattern.replace(nextToken[0],'');
                        nextToken = pattern.match(/\[(.*?)\]/);
                    }
                }
                
                var rowPackageType =_.find(FIELD_OPTIONS.repoPackageTypes, (type) => {
                    return type.serverEnumName == repo.repoType;
                });
                if (rowPackageType)
                    repo.repoTypeIcon = rowPackageType.icon;
                
                return repo;
            });
            this.comm.localRepo = _.findWhere(this.comm.reposList, {repoKey: this.node.data.repoKey});
        });
        this.comm.createNotification = this.createNotification.bind(this);
        this.comm.setController = (controller) => {
            this.deployController = controller;
        };
    }

    /**
     * deploy on selected controller (single or multi)
     */
    deploy() {
        this.deployController.deployArtifacts();
    }

    /**
     * when deploy success refresh node and close modal
     */
    onDeploySuccess() {
        this.dispatchSuccessEvent();
        this.modalInstance.close();
    }

    /**
     * check if current deploy selected
     * @param deploy
     * @returns {boolean}
     */
    isSelectedDeploy(deploy) {
        return this.currentDeploy === deploy;
    }

    /**
     * This builds an appropriate notification for the Deploy action in the UI (with or w/o the Artifact URL)
     *
     * @param response from the server
     * @returns {{type: string, body: string}}
     */
    createNotification(response) {
        let {repoKey, artifactPath} = response;
        artifactPath = _.trim(artifactPath, '/');
        let messageWithUrl = `Successfully deployed <a href="#/artifacts/browse/tree/General/${repoKey}/${artifactPath}">${artifactPath}</a> to ${repoKey}`;
        let messageWithoutUrl = `Successfully deployed ${artifactPath}`;
        return {
            type: 'success',
            body: response.showUrl ? messageWithUrl : messageWithoutUrl
        }
    }

    /**
     * after deploy _dispatchSuccessEvent refresh node in tree
     * @private
     */
    dispatchSuccessEvent() {
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_DEPLOY, this.deployFile.repoDeploy.repoKey);
    }

    onRepoChange() {
        this.$timeout(()=>{
            if (this.deployController.layoutTokens) {
                delete this.deployController.layoutTokens;
                this.deployController.extractTokensFromLayout();
                this.deployController.updatePathFromLayoutTokens();
            }

            let repo = this.deployController.deployFile.repoDeploy;
            if (repo.repoType !== 'Maven' && this.deployController.deployFile.unitInfo) {
                delete this.deployController.deployFile.unitInfo.maven;
                this.deployController.deployFile.unitInfo.mavenArtifact = false;
                if (this.deployController.originalDeployPath) {
                    this.deployController.deployFile.targetPath = angular.copy(this.deployController.originalDeployPath);
                }
            }
        });
    }

}
