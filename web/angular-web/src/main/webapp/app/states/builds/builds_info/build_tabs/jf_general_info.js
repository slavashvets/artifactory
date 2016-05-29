import DICTIONARY from "./../../constants/builds.constants";

class jfGeneralInfoController {
    constructor($scope, $stateParams, BuildsDao, PushToBintrayModal, User, DistributionDao, ArtifactoryModal) {
        this.$scope = $scope;
        this.$stateParams = $stateParams;
        this.buildsDao = BuildsDao;
        this.pushToBintrayModal = PushToBintrayModal;
        this.generalData = {};
        this.distributionDao = DistributionDao;
        this.DICTIONARY = DICTIONARY.generalInfo;
        this.User = User;
        this.modal = ArtifactoryModal;

        //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
        // this.userCanPushToBintray = false;
        this.userCanDistribute = false;

        this._getGeneralInfo();
    }

    pushToBintray() {
        this.modalInstance = this.pushToBintrayModal.launchModal('build');
    }

    distribute() {
        this.distributionDao.getAvailableDistributionRepos({}).$promise.then((data)=>{
            let modalInstance;
            this.distributeModalScope = this.$scope.$new();

            this.distributeModalScope.title = "Distribute " + this.$stateParams.buildName + " #" + this.$stateParams.buildNumber;
            this.distributeModalScope.distributionRepositoriesOptions = _.map(data, 'repoKey');

            this.distributeModalScope.data = {};
            this.distributeModalScope.data.async = true;
            this.distributeModalScope.data.publish = true;
            this.distributeModalScope.data.publish = true;
            this.distributeModalScope.data.overrideExistingFiles = false;
            this.distributeModalScope.data.selectedRepo = null;
            this.distributeModalScope.distType = "build";

            this.distributeModalScope.distribute = () => {
                this.distributionDao.distributeBuild({
                    targetRepo: this.distributeModalScope.data.selectedRepo,
                    async: this.distributeModalScope.data.async,
                    overrideExistingFiles: this.distributeModalScope.data.overrideExistingFiles
                },{
                    buildName: this.$stateParams.buildName,
                    buildNumber: this.$stateParams.buildNumber,
                    date: this.$stateParams.startTime
                }).$promise.then((res)=>{
                    // Success
                    modalInstance.close();
                });
            };

            modalInstance = this.modal.launchModal('distribute_modal', this.distributeModalScope, 'sm');
        });
    }

    _getGeneralInfo() {
        return this.buildsDao.getData({
            name: this.$stateParams.buildName,
            number: this.$stateParams.buildNumber,
            time: this.$stateParams.startTime,
            action:'buildInfo'
        }).$promise.then((data) => {
            //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
            // this.userCanPushToBintray = data.allowPushToBintray && this.User.getCurrent().canPushToBintray();
            this.userCanDistribute = data.userCanDistribute;
            this.generalData = data;
        });

    }

}


export function jfGeneralInfo() {
    return {
        restrict: 'EA',
        controller: jfGeneralInfoController,
        controllerAs: 'jfGeneralInfo',
        scope: {},
        bindToController: true,
        templateUrl: 'states/builds/builds_info/build_tabs/jf_general_info.html'
    }
}