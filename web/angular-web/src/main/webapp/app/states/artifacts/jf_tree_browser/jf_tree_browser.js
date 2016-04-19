import TreeConfig from './jf_tree_browser.config';
import EVENTS     from '../../../constants/artifacts_events.constants';
import JFCommonBrowser from '../jf_common_browser/jf_common_browser';
/**
 * wrapper around the jstree jquery component
 * @url http://www.jstree.com/
 *
 * @returns {{restrict: string, controller, controllerAs: string, bindToController: boolean}}
 */
export function jfTreeBrowser() {
    return {
        scope: {
            browserController: '='
        },
        restrict: 'E',
        controller: JFTreeBrowserController,
        controllerAs: 'jfTreeBrowser',
        templateUrl: 'states/artifacts/jf_tree_browser/jf_tree_browser.html',
        bindToController: true,
        link: function ($scope) {
            $scope.jfTreeBrowser.initJSTree();
        }
    }
}

const ARCHIVE_MARKER = '!';

class JFTreeBrowserController extends JFCommonBrowser {
    constructor($timeout, $compile, ArtifactoryEventBus, $element, $scope, TreeBrowserDao, $stateParams, $q, ArtifactoryState, ArtifactActions, ArtifactoryNotifications, NativeBrowser, User, AdvancedStringMatch) {
        super(ArtifactActions, AdvancedStringMatch);
        this.type="tree";
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.$compile = $compile;
        this.$stateParams = $stateParams;
        this.$q = $q;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.treeBrowserDao = TreeBrowserDao;
        this.artifactoryState = ArtifactoryState;
        this.user = User.currentUser;
        this.nativeBrowser = NativeBrowser;
        if (_.isEmpty($stateParams.artifact)) {
            // Important to know for switching to simple browser
            this.whenTreeDataLoaded = $q.when([]);
        }
        else {
            this.whenTreeDataLoaded = TreeBrowserDao.findNodeByFullPath($stateParams.artifact); // Preload data for the current selected artifact
        }

        this.$element = $element;

        let doRefresh = this.artifactoryState.getState('refreshTreeNextTime');
        if (doRefresh) {
            $timeout(()=>this._refreshTree());
            this.artifactoryState.setState('refreshTreeNextTime',false);
        }

    }


    /****************************
     * Init code
     ****************************/

    // This is called from link function
    initJSTree() {
        // preload artifact
        this.whenTreeDataLoaded.then(() => {
            this.treeElement = $(this.$element).find('#tree-element');
            this._registerEvents();
            this._buildTree();
            this._registerTreeEvents();
        });
    }

    /**
     * When JStree is ready load the current browsing path from URL
     * and restore the nodes open and selected state.
     * @param e
     * @private
     */
    _openTreeNode(artifact) {
        let deferred = this.$q.defer();
        let jstree = this.jstree();
        let root = jstree.get_node('#');
        let path = _.trim(artifact?artifact.replace('//', '/'):'', '/').split('/');

        this._openNodePath(root, path, jstree.get_node(root.children[0]), (selectedNode) => {
            jstree.deselect_all();
            // Select the node
            jstree.select_node(selectedNode);

            // scroll the node into view
            let domElement = this._getDomElement(selectedNode);
            this._scrollIntoView(domElement);
            this._focusOnTree();
            deferred.resolve();
        });
        return deferred.promise
    }

    _onReady() {

        this.$timeout(()=>{
            this._initTrashPin();
        },100);

        this._openTreeNode(this.$stateParams.artifact);
        this.jstree().show_dots();
    }

    /****************************
     * Event registration
     ****************************/
    _registerEvents() {
        // Must destroy jstree on scope destroy to prevent memory leak:
        this.$scope.$on('$destroy', () => {
            if (this.jstree()) {
                this.jstree().destroy();
            }
            $(window).off('resize');
        });

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_SEARCH_CHANGE, text => this._searchTree(text));
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_SEARCH_CANCEL, text => this._clear_search());
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_SEARCH_KEYDOWN, key => this._searchTreeKeyDown(key));
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ACTION_DEPLOY, repoKey => this._refreshRepo(repoKey));
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ACTION_REFRESH, node => this._refreshFolder(node));
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_REFRESH, (node) => node ? this._refreshFolder(node) : this._refreshTree());
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ACTION_DELETE, (node) => {
            this._refreshParentFolder(node); // Refresh folder of node's parent
            this.refreshTrashCan();
        });
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ACTION_MOVE, (options) => {
            this._refreshParentFolder(options.node); // Refresh folder of node's parent
            this._refreshFolderPath(options); // Refresh target folder where node was copied
        });
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ACTION_COPY, (options) => {
            this._refreshFolderPath(options);
        });

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_NODE_OPEN, path => {
            this._openTreeNode(path)
        });

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_COLLAPSE_ALL, () => {
            this._collapseAll();
        });

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_COMPACT, () => this._toggleCompactFolders());

        // URL changed (like back button / forward button / someone input a URL)
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.ARTIFACT_URL_CHANGED, (stateParams) => {
            if (stateParams.browser != 'tree') return;
            // Check if it's already the current selected node (to prevent opening the same tree node twice)
            let selectedNode = this._getSelectedTreeNode();
            if (selectedNode && selectedNode.fullpath === stateParams.artifact) return;
            this.treeBrowserDao.findNodeByFullPath(stateParams.artifact)
                .then(() => this._openTreeNode(stateParams.artifact));
        });

        $(window).on('resize', () => this._resizePinnedTrash());
        this.$scope.$on('ui-layout.resize', () => this._resizePinnedTrash());


    }

    /**
     * register a listener on the tree and delegate to
     * relevant methods
     *
     * @param element
     * @private
     */
    _registerTreeEvents() {
//        $(this.treeElement).on("search.jstree", (e, data) => this._onSearch(e, data));
        $(this.treeElement).on("ready.jstree", (e) => this._onReady(e));

        $(this.treeElement).on("select_node.jstree", (e, args) => {
            if (args.event) { // User clicked / pressed enter
                this.artifactoryState.setState('tree_touched', true);
            }
            this._loadNode(args.node);
        });
        $(this.treeElement).on("activate_node.jstree", (e, args) => {
            if (args.event) { // User clicked / pressed enter
                this.artifactoryState.setState('tree_touched', true);
            }

            if (!args.node.data.isArchive() && args.node.data.icon !== 'docker') this.jstree().open_node(args.node);
        });

        $(this.treeElement).on("after_open.jstree",(e, args)=>{
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash) this._initTrashPin();

            this._focusOnTree();
            if (this.activeFilter) this._searchTree(this.searchText,false,false);

            if (args.node.$autoScroll) {
                this._autoScroll(args.node);
                delete args.node.$autoScroll;
            }

        });

        $(this.treeElement).on("click",(e) => {
            let node = this.jstree().get_node($(e.target));
            let nodeIsTrash = node.data && node.data.isTrashcan && node.data.isTrashcan();
            let nodeIsInTrash = node.data && node.data.isInTrashcan && node.data.isInTrashcan();

            if ((!nodeIsTrash && !nodeIsInTrash) || !this.isTrashPinned()) {
                node.$autoScroll = true;
            }
        });


        $(this.treeElement).on("after_close.jstree",(e, args)=> {
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash) this._initTrashPin();
        });
        $(this.treeElement).on("load_node.jstree",(e, args)=> {
            let nodeIsTrash = args.node && args.node.data && args.node.data.isTrashcan && args.node.data.isTrashcan();
            if (nodeIsTrash || args.node.id === '#') this._initTrashPin();
        });

        $('#tree-element').on('keydown',(e) => {
            if (e.keyCode === 37 && e.ctrlKey) { //CTRL + LEFT ARROW --> Collapse All !
                this._collapseAll();
            }
        });

        $(this.treeElement).scroll(()=>this._onScroll());


    }

    _autoScroll(node) {
        let treeItem = $('#' + node.id).find('.jstree-children')[0];
        if (!treeItem) return;

        let numOfChildrens = $(treeItem).find('> div').length,
                heightOfElement = $('.jstree-anchor').first().height(),
                treeWrapperHeight = $('#tree-element').offset().top + $('#tree-element').height();

        if ($(treeItem).offset().top + (heightOfElement *2) > treeWrapperHeight) {
            let currentScroll = $('#tree-element').scrollTop();

            if (numOfChildrens > 3) {
                this.$timeout(()=> {
                    $('#tree-element').animate({scrollTop: currentScroll + ((heightOfElement * 3) + (heightOfElement/2) + 5)})
                });
            }
            if (numOfChildrens <= 3) {
                this.$timeout(()=> {
                    $('#tree-element').animate({scrollTop: currentScroll + (heightOfElement * numOfChildrens)});
                });
            }
        }
    }
    _resizePinnedTrash() {
        let e = $('.jstree-li-the-trashcan');
        if (e.hasClass('pinned')) {
            let p = e.parent().parent();
            e.css('width',p.outerWidth()+'px');

            this.treeElement.css('height','auto');
            var h = parseInt(this.treeElement.css('height'));
            this.treeElement.css('height',h-e.height() + 'px');

        }
        else {
            e.css('width','auto');
            this.treeElement.css('height','auto');
        }

        let trashPin = $('.trash-pin');
        trashPin.css('left', e.parent().parent().width() - 50 + 'px')


    }

    _collapseAll() {
        let node = this.jstree().get_selected(true)[0];

        let parentRepoNode = this.jstree().get_node(node.parent);
        if (parentRepoNode.id === '#') parentRepoNode = node;
        else {
            while (parentRepoNode.parent !== '#') {
                parentRepoNode = this.jstree().get_node(parentRepoNode.parent);
            }
        }

        $('#tree-element').jstree('close_all');
        this.jstree().select_node(parentRepoNode);
        this.$timeout(()=>{
            this.jstree().close_node(parentRepoNode);
        });
    }

    _loadNode(item) {
        item.data.load().then(() => {
            this.artifactoryEventBus.dispatch(EVENTS.TREE_NODE_SELECT, item)
        });
    }

    /****************************
     * Compact folders
     ****************************/
    _toggleCompactFolders() {
        this._refreshTree();
    }

    /****************************
     * Building the tree
     ****************************/
    _buildTree() {
        let asyncStateLoad = (obj, cb) => {
            let promise;
            if (obj.id === '#') {
                promise = this.treeBrowserDao.getRoots();
            }
            else {
                promise = obj.data.getChildren();
            }
            promise.then((data) => {
                if (obj.id === '#') {
                    data.forEach((node)=>{
                        let removeIndex = node.actions.indexOf(_.findWhere(node.actions, {name: "NativeBrowser"}));
                        if (removeIndex !== -1) node.actions.splice(removeIndex,1);

                        let index = node.actions.indexOf(_.findWhere(node.actions, {name: "Watch"}));
                        if (index===-1) index = node.actions.indexOf(_.findWhere(node.actions, {name: "Unwatch"}));
                        if (index===-1) index = node.actions.indexOf(_.findWhere(node.actions, {name: "Move"}));
                        if (index===-1) index = node.actions.indexOf(_.findWhere(node.actions, {name: "Refresh"}));
                        if (this.nativeBrowser.isAllowed(node)) node.actions.splice(index+1,0,{
                            icon: "icon-simple-browser",
                            name: "NativeBrowser",
                            title: "Native Browser"
                        });

                    })
                }
                this.artifactoryState.setState("hasArtifactsData", data.length > 0 || obj.id !== '#');
                cb(this._transformData(data));
            });
        };

        TreeConfig.core.data = asyncStateLoad;
        TreeConfig.contextmenu.items = this._getContextMenuItems.bind(this);

                // Search by node text only (otherwise searches the whole HTML)
        TreeConfig.search.search_callback = this._searchCallback.bind(this);

        $(this.treeElement).jstree(TreeConfig);
    }

    _transformData(data) {
        data = data || [];
        return data.map((node) => {
            let item = {};
            item.children = node.hasChild;
            item.text = node.isTrashcan() ? '<span class="trashcan-node">Trash Can<i ng-show="!jfTreeBrowser.isTrashPinned()" ng-click="jfTreeBrowser.toggleTrashPin($event)" class="icon icon-pin trash-pin" jf-tooltip="Pin Trash Can"></i><i ng-show="jfTreeBrowser.isTrashPinned()" ng-click="jfTreeBrowser.toggleTrashPin($event)" class="icon icon-unpin trash-pin" jf-tooltip="Unpin Trash Can"></i></span>'
                : node.text;
            item.data = node;
            item.type=node.iconType;
            if (node.isTrashcan())
                item.li_attr={class:"-the-trashcan"};
            else if (node.isCachedExternalDependency())
                item.li_attr={class:"-cached-external-dependency"};
            return item;
        });
    }

    /****************************
     * Refreshing the tree
     ****************************/

    /**
     * refresh children of folder
     *
     * @param node
     * @private
     */
    _refreshRepo(repoKey) {
        let jstree = this.jstree();
        let root = jstree.get_node('#');
        let repoJsNode;
        _.each(root.children, (child) => {
            repoJsNode = jstree.get_node(child);
            if (repoJsNode && repoJsNode.data && repoJsNode.data.repoKey === repoKey) return false;
        });
        //console.log(repoJsNode.data.repoKey);
        if (repoJsNode) {
            repoJsNode.data.invalidateChildren();
            jstree.load_node(repoJsNode, () => {
                jstree.select_node(repoJsNode);
            });
        }
    }

    _refreshFolder(node) {
        if (node.data) node.data.invalidateChildren();
        else this.treeBrowserDao.invalidateRoots();
        this.jstree().load_node(node);
    }

    _refreshParentFolder(node) {
        node.data.invalidateParent();
        let parentNodeItem = this.jstree().get_node(node.parent);
        this.$timeout(() => {        
            this._refreshFolder(parentNodeItem);
            this.jstree().select_node(parentNodeItem);
        }, 500);
    }

    _refreshFolderPath(option) {
        let targetPath = _.compact(option.target.targetPath.split('/'));
        let path = [option.target.targetRepoKey].concat(targetPath);

        let curNode = this.jstree().get_node('#');

        let childNode = this._getChildByPath(curNode, path);
        if (childNode && _.isArray(childNode.children)) {
            curNode = childNode;
        }

        // Data is still not refreshed on server
        this.$timeout(()=> {
            if (curNode && curNode.data) {
                this._refreshFolder(curNode);
                curNode.data.getChildren().then(()=> {
                    this._openTreeNode(option.target.targetRepoKey + '/' + option.target.targetPath + '/' + option.node.data.text)
                });
            }
            else {
                this._openTreeNode(option.target.targetRepoKey + '/' + option.target.targetPath + '/' + option.node.data.text);
            }
        }, 500);
    }

    _refreshTree() {
        this.treeBrowserDao.invalidateRoots();
        if (this.jstree() && this.jstree().refresh) this.jstree().refresh();
    }

    /****************************
     * Traversing the tree
     ****************************/

     /**
     * Find the next child by path. Take into account the node's text by consist of some of the path elements (in compact mode)
     * @param parentNode:Object node object from where to start
     * @param path:Array array of path elements
     * @returns childNode or undefined
     * @private
     */    
    _getChildByPath(parentNode, path) {
        let jstree = this.jstree();
        let children = this._getChildrenOf(parentNode);
        // Find the node that conforms to the largest subpath of path 
        for(let i = path.length; i > 0; i--) {
            let subpath = path.slice(0, i);
            let testPathStr = _.trimRight(subpath.join('/'), ARCHIVE_MARKER);
            let result = _.find(children, (childNode) => {
                // Sometimes the node's text is not the full text (like for docker images)
                let childPath = childNode.data.fullpath;

                if (childPath === testPathStr || childPath === testPathStr + '/') {
                    return childNode;
                }
            });
            if (result) return result;
        }
    }

    _getChildrenOf(parentNode) {
        let jstree = this.jstree();
        return _.compact(parentNode.children.map((jsTreeNodeId) => jstree.get_node(jsTreeNodeId)));
    }

    /**
     * Open the path starting from the root node, and call the callback with the leaf node
     * and restore the nodes open and selected state.
     * @param node:Object node object from where to start
     * @param path:Array array of path elements
     * @param selectedNode:Object default node to return if not found
     * @param callback:Function callback to call with leaf node once the traversing is complete
     * @private
     */
    _openNodePath(node, path, leafNode, callback, pathStopIndex = 1) {
        let jstree = this.jstree();
        let childNode;
        while(pathStopIndex <= path.length) {
            let testPath = path.slice(0, pathStopIndex);
            childNode = this._getChildByPath(node, testPath);
            if (childNode) break;
            pathStopIndex++;
        }

        if (childNode) {
            leafNode = childNode;
            if (path.length === 0) {
                callback(leafNode);
            }
            else {
                if (!leafNode.data.isArchive() && leafNode.data.icon !== 'docker') {
                    jstree.open_node(leafNode, (node) => {
                        this._openNodePath(leafNode, path, leafNode, callback, pathStopIndex + 1);
                    }, false);
                }
                else {
                    callback(leafNode);
                }
            }
        }
        else {
            callback(leafNode);
        }
    }
    refreshTrashCan() {
        let trashID = $('.trashcan-node').parent().parent().prop('id');
        let trashNode = this.jstree().get_node(trashID);
        this.artifactoryEventBus.dispatch(EVENTS.TREE_REFRESH, trashNode);
    }


    pinTrash() {
        let trashElem = $('.jstree-li-the-trashcan');
        if (!trashElem.length) return;

        let trashPin = $('.trash-pin');
        this.tempScrollTop = trashElem.scrollParent().scrollTop();
        let wasPinned = trashElem.hasClass('pinned');
        trashElem.addClass('pinned');
        localStorage.pinnedTrash = true;

        this.$scope.$broadcast('ui-layout.resize');

        trashElem.offset({left: trashElem.closest('#tree-element').offset().left});
        $(trashElem.children()[0]).css('margin-left','-10px');
        $(trashElem.find('.jstree-children')[0]).css('margin-left','-10px');
        trashElem.on('scroll', () => {
            let target = trashElem.scrollTop();
//            trashPin.css('top',target);
            trashPin.css('left', trashElem.scrollLeft() + trashElem.parent().parent().width() - 50 + 'px')
        });
        if (!wasPinned) trashPin.scrollParent().scrollTop(0);

    }

    unpinTrash() {
        let trashElem = $('.jstree-li-the-trashcan');
        if (!trashElem.length) return;

        let trashPin = $('.trash-pin');
        trashElem.removeClass('pinned');
        delete localStorage.pinnedTrash;
//        trashPin.css('top','auto');
        trashElem.scrollParent().scrollTop(this.tempScrollTop);
        $(trashElem.children()[0]).css('margin-left','0px');
        $(trashElem.find('.jstree-children')[0]).css('margin-left','0px');

        if (!this._isScrolledIntoView(trashElem.find('.jstree-anchor')[0],0)) {
            this._scrollIntoView($(trashElem.find('.jstree-anchor')[0]));
        }

        this.$scope.$broadcast('ui-layout.resize');
    }

    toggleTrashPin(e) {
        e.stopImmediatePropagation()
        e.preventDefault();

        let trashElem = $('.jstree-li-the-trashcan');
        if (trashElem.hasClass('pinned')) {
            this.unpinTrash();
        }
        else {
            this.pinTrash();
       }
    }

    _initTrashPin() {
        let e = $('.trash-pin');
        if (!e.prop('compiled')) {
            this.$compile(e)(this.$scope);
            e.prop('compiled',true);
        }

        if (this.isTrashPinned()) {
            this.pinTrash();
        }
        this.$scope.$broadcast('ui-layout.resize');
    }

    isTrashPinned() {
        return localStorage.pinnedTrash;
    }

}