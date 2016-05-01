import EVENTS   from '../../../constants/artifacts_events.constants';
import TOOLTIPS from '../../../constants/artifact_tooltip.constant';
import ICONS from '../constants/artifact_browser_icons.constant';
import ACTIONS from '../../../constants/artifacts_actions.constants';

export class ArtifactsController {
    constructor($scope, $state, ArtifactoryEventBus, ArtifactoryState, SetMeUpModal, ArtifactoryDeployModal, User, ArtifactActions) {

        this.artifactoryEventBus = ArtifactoryEventBus;
        this.$state = $state;
        this.$scope = $scope;
        this.node = null;
        this.deployModal = ArtifactoryDeployModal;
        this.setMeUpModal = SetMeUpModal;
        this.artifactoryState = ArtifactoryState;
        this.tooltips = TOOLTIPS;
        this.icons = ICONS;
        this.artifactActions = ArtifactActions;

        this.user = User.getCurrent();

        this.initEvents();
    }


    getNodeIcon() {
        if (this.node && this.node.data) {
            let type = this.icons[this.node.data.iconType];
            if (!type) type = this.icons['default'];
            return type && type.icon;
        }
    }


    openSetMeUp() {
        this.setMeUpModal.launch(this.node);
    }

    openDeploy() {
        this.deployModal.launch(this.node);
    }

    initEvents() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_NODE_SELECT, node => this.selectNode(node));

        this.artifactoryEventBus.registerOnScope(this.$scope, [EVENTS.ACTION_WATCH, EVENTS.ACTION_UNWATCH], () => {
            this.node.data.refreshWatchActions()
                .then(() => {
                    this.actionsController.setActions(this.node.data.actions);
                });
        });

    }

    selectNode(node) {

        let previousNode = this.node;
        this.node = node;

        if (node.data) {
            this.artifactoryState.setState('repoKey', this.node.data.repoKey);
            let location = true;
            if (this.$state.current.name === 'artifacts.browsers.path' && (!previousNode || (!this.$state.params.artifact && this.$state.params.tab !== 'StashInfo'))) {
                // If no artifact and selecting artifact - replace the location (fix back button bug)
                location = 'replace';
            }
            this.$state.go(this.$state.current, {artifact: node.data.fullpath}, {location: location});

            this.actionsController.setCurrentEntity(node);
            this.node.data.getDownloadPath()
                .then(() => {
                    let downloadAction = _.findWhere(node.data.actions,{name: 'Download'});
                    if (downloadAction) {
                        downloadAction.href = node.data.actualDownloadPath;
                    }
                    this.actionsController.setActions(node.data.actions)
                });
        }
        else {
            this.artifactoryState.removeState('repoKey');
            this.$state.go(this.$state.current, {artifact: ''});
            this.actionsController.setActions([]);
        }
    }

    exitStashState() {
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_EXIT_STASH);
    }

    hasData() {
        return this.artifactoryState.getState('hasArtifactsData') !== false;
    }

    initActions(actionsController) {
        this.actionsController = actionsController;
        actionsController.setActionsHandler(this.artifactActions);
        actionsController.setActionsDictionary(ACTIONS);
    }
}