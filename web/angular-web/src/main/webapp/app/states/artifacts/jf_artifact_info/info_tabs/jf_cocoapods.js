import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';
class jfCocoapodsController {
    constructor($scope, ArtifactViewsDao, ArtifactoryEventBus, ArtifactoryGridFactory) {
        this.artifactViewsDao = ArtifactViewsDao;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.DICTIONARY = DICTIONARY.cocoapods;
        this.gridDependenciesOptions = {};
        this.cocoapodsData = {};
        this.$scope = $scope;
        this._initCocoapods();
    }

    _initCocoapods() {
        this._registerEvents();
        this.getCocoapodsData();
    }

    getCocoapodsData() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "cocoapods",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise
            .then((data) => {
                this.cocoapodsData = data;
                this._createGrid();
            });
    }

    _createGrid() {
        if (this.cocoapodsData.dependencies) {
            if (!Object.keys(this.gridDependenciesOptions).length) {
                this.gridDependenciesOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setRowTemplate('default')
                    .setColumns(this._getColumns())
                    .setGridData(this.cocoapodsData.dependencies)
            }
            else {
                this.gridDependenciesOptions.setGridData(this.cocoapodsData.dependencies)
            }
        }
    }

    _getColumns() {
        return [{
            name: 'Name',
            displayName: 'Name',
            field: 'name'
        },
            {
                name: 'Version',
                displayName: 'Version',
                field: 'version'
            }];
    }

    _registerEvents() {

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            if (this.currentNode != node) {
                this.currentNode = node;
                this.getCocoapodsData();
            }
        });
    }

}
export function jfCocoapods() {
    return {
        restrict: 'EA',
        controller: jfCocoapodsController,
        controllerAs: 'jfCocoapods',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_cocoapods.html'
    }
}