import ACTIONS from '../../../../constants/user_actions.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityUserFormController {
    constructor($scope, $state, $stateParams, $timeout, $q, ArtifactoryGridFactory, UserDao, GroupsDao, GroupPermissionsDao, AdminSecurityGeneralDao, User,
            uiGridConstants, commonGridColumns, ArtifactoryModelSaver, RepositoriesDao, UserProfileDao, ArtifactoryModal, ArtifactoryNotifications) {

        this.$scope = $scope;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.$timeout = $timeout;
        this.$q = $q;
        this.User = User;
        this.repositoriesDao = RepositoriesDao;
        this.adminSecurityGeneralDao = AdminSecurityGeneralDao;
        this.modal = ArtifactoryModal;
        this.userDao = UserDao.getInstance();
        this.groupsDao = GroupsDao.getInstance();
        this.groupPermissionsDao = GroupPermissionsDao.getInstance();
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['userdata','input'],['locked']);
        this.permissionsGridOptions = {};
        this.userProfileDao = UserProfileDao;
        this.uiGridConstants = uiGridConstants;
        this.commonGridColumns = commonGridColumns;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.TOOLTIP = TOOLTIP.admin.security.usersForm;
        this.input = {};

        this._getPasswordExpirationState();

        if ($stateParams.username) {
            this.mode = 'edit';
            this.username = $stateParams.username;
            this.title = 'Edit ' + this.username + ' User';
            this._getUserData();
            this._getUserPermissions();
        }
        else {
            this.mode = 'create';
            this.title = 'Add New User';
            this.userdata = {
                groups: [],
                profileUpdatable: true
            };
            this.userPermissions = [];
        }

        this._createGrid();

        this._getAllRepos();
        this._getAllGroups();
        this._getGroupsPermissions();
    }

    _getAllRepos() {
        this.reposData = {};
        this.repositoriesDao.getRepositories({type:'local'}).$promise
                .then((data) => {
                    this.reposData.locals = _.map(data,(r)=>{return r.repoKey;});
                });
        this.repositoriesDao.getRepositories({type:'remote'}).$promise
                .then((data) => {
                    this.reposData.remotes = _.map(data,(r)=>{return r.repoKey;});
                });
    }

    _createGrid() {
        this.permissionsGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
            .setColumns(this._getPermissionCloumns())
            .setRowTemplate('default');

    }

    _getAllGroups() {
        this.groupsDao.getAll().$promise.then((data)=> {
            this.groupsData = data;
            this.groupsList = _.map(this.groupsData, (group)=> {
                if (group.autoJoin && this.mode === 'create') {
                    this.userdata.groups.push(group.groupName);
                    this.artifactoryModelSaver.save();
                }
                return group.groupName;
            });
            if (this.mode === 'create') this._getGroupsPermissions();
        });
    }


    _getUserPermissions() {
        this.userDao.getPermissions({userOnly: true}, {name: this.username}).$promise.then((data)=> {
            /*
            let filteredData = _.filter(data, (perm) => {
                return perm.effectivePermission.principal === this.username;
            });
             */
            this.userPermissions = data;//filteredData;
            if (this.groupsPermissions) this._setGridData();
        });
    }

    _getGroupsPermissions() {
        if (!this.userdata) return;
        if (!this.userdata.groups || !this.userdata.groups.length) {
            this.groupsPermissions = [];
            if (this.mode==='create') this.permissionsGridOptions.setGridData(this.groupsPermissions);
            else if (this.userPermissions) this._setGridData();
            return;
        }
        this.groupPermissionsDao.get({groups: this.userdata.groups}).$promise.then((data)=> {
            this.groupsPermissions = data;
            if (this.mode==='create') this.permissionsGridOptions.setGridData(this.groupsPermissions);
            else if (this.userPermissions) this._setGridData();
        });
    }

    _setGridData() {
        let data = angular.copy(this.groupsPermissions);
        data = data.concat(this.userPermissions);
        this._fixDataFormat(data).then((fixedData)=>{
            this.permissionsGridOptions.setGridData(fixedData);
        });
    }

    _fixDataFormat(data,defer = null) {
        let defer = defer || this.$q.defer();
        if (this.reposData.locals && this.reposData.remotes) {
            data.forEach((record)=>{
                if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY LOCAL') {
                    record.repoKeysView = 'ANY LOCAL';
                    record.reposList = angular.copy(this.reposData.locals);
                }
                else if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY REMOTE') {
                    record.repoKeysView = 'ANY REMOTE';
                    record.reposList = angular.copy(this.reposData.remotes);
                }
                else if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY') {
                    record.repoKeysView = 'ANY';
                    record.reposList = angular.copy(this.reposData.remotes).concat(this.reposData.locals);
                }
                else {
                    record.repoKeysView = record.repoKeys.join(', ');
                    record.reposList = angular.copy(record.repoKeys);
                }
            });
            defer.resolve(data);
        }
        else {
            this.$timeout(()=>{
                this._fixDataFormat(data,defer);
            })
        }
        return defer.promise;
    }

    _getUserData() {

        this.userDao.getSingle({name: this.username}).$promise.then((data) => {
            //console.log(data);
            this.userdata = data;
            if (!this.userdata.groups) this.userdata.groups = [];
            this.artifactoryModelSaver.save();
            this._getGroupsPermissions();
        });
        this._getApiKeyState();
    }

    _getApiKeyState() {
        this.userProfileDao.getApiKey({},{username: this.username}).$promise.then((res)=>{
            this.apiKeyExist = !!res.apiKey;
        });
    }

    _fixGroups(userdata) {
        let groups = userdata.groups;
        let groupsObjects = [];
        groups.forEach((group)=> {
            let realm = _.findWhere(this.groupsData, {groupName: group}).realm;
            groupsObjects.push({groupName: group, realm: realm});
        });
        delete(userdata.groups);
        userdata.userGroups = groupsObjects;
    }

    updateUser() {
        let payload = angular.copy(this.userdata);
        _.extend(payload, this.input);
        this._fixGroups(payload);
        this.userDao.update({name: this.userdata.name}, payload).$promise.then((data) => {
            if (this.userdata.name === this.User.currentUser.name) this.User.reload();
            this.artifactoryModelSaver.save();
            this.$state.go('^.users');
        });
    }

    createNewUser() {
        let payload = angular.copy(this.userdata);
        _.extend(payload, this.input);
        this._fixGroups(payload);
        this.userDao.create(payload).$promise.then((data) => {
            this.artifactoryModelSaver.save();
            this.$state.go('^.users');
        });
    }

    save() {
        if (this.mode == 'edit')
            this.updateUser();
        if (this.mode == 'create')
            this.createNewUser();
    }

    cancel() {
        this.$state.go('^.users');
    }

    deleteUser() {
        let json = {userNames:[this.username]};
        this.modal.confirm(`Are you sure you want to delete user '${this.username}?'`)
            .then(() => this.userDao.delete(json).$promise.then(()=>this.cancel()));
    }

    onChangeGroups() {
        this.userPermissions = undefined;
        this.groupsPermissions = undefined;
        this._getGroupsPermissions();
        if (this.mode === 'edit') this._getUserPermissions();
    }

    onClickAdmin() {
        if (this.userdata.admin) {
            this.userdata.profileUpdatable = true;
            this.userdata.internalPasswordDisabled = false;
        }
    }

    _getPermissionCloumns() {

        let nameCellTemplate = '<div class="ui-grid-cell-contents"><a href ui-sref="admin.security.permissions.edit({permission: row.entity.permissionName})">{{row.entity.permissionName}}</a></div>';

        return [
            {
                field: "permissionName",
                name: "Permission Target",
                displayName: "Permission Target",
                sort: {
                    direction: this.uiGridConstants.ASC
                },
                cellTemplate: nameCellTemplate,
                width:'16%'
            },
            {
                field: "effectivePermission.principal",
                name: "Applied To",
                displayName: "Applied To",
                width: '13%'
            },
            {
                field: "repoKeys",
                name: "Repositories",
                displayName: "Repositories",
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.reposList','row.entity.permissionName','row.entity.repoKeysView',true),
                width:'16%'

            },
            {
                field: "effectivePermission.managed",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.managed'),
                name: "Manage",
                displayName: "Manage",
                width:'9%'
            },
            {
                field: "effectivePermission.delete",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.delete'),
                name: "Delete/Overwrite",
                displayName: "Delete/Overwrite",
                width:'15%'
            },
            {
                field: "effectivePermission.deploy",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.deploy'),
                name: "Deploy/Cache",
                displayName: "Deploy/Cache",
                width:'14%'
            },
            {
                field: "effectivePermission.annotate",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.annotate'),
                name: "Annotate",
                displayName: "Annotate",
                width:'9%'
            },
            {
                field: "effectivePermission.read",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.read'),
                name: "Read",
                displayName: "Read",
                width:'8%'
            }
        ]

    }

    isSaveDisabled() {
        return !this.userForm || this.userForm.$invalid || ((this.input.password || this.input.retypePassword) && (this.input.password !== this.input.retypePassword));
    }

    checkPwdMatch(retypeVal) {
        return !retypeVal || (retypeVal && this.input.password === retypeVal);
    }

    isAnonymous() {
        return this.userdata.name === 'anonymous';
    }

    revokeApiKey() {
        this.modal.confirm(`Are you sure you want to revoke API key for this user ?`)
                .then(() => {
                    this.userProfileDao.revokeApiKey({}, {username: this.username}).$promise.then(()=>{
                        this.apiKeyExist = false;
                    });

                });
    }

    unlockUser() {
        this.adminSecurityGeneralDao.unlockUsers({},[this.username]).$promise.then((res)=>{
            if(res.status === 200) {
                this.userdata.locked = false;
            }
        });
    }


    expirePassword() {
        this.modal.confirm(`Are you sure you want to expire this user's password?`)
            .then(() => {
                this.userDao.expirePassword({}, {username: this.username}).$promise.then(()=> {
                    this._getUserData();
                })
            });
    }

    unexpirePassword() {
        this.userDao.unExpirePassword({}, {username: this.username}).$promise.then(()=> {
            this._getUserData();
        })
    }

    _getPasswordExpirationState() {
        this.adminSecurityGeneralDao.get().$promise.then((data) => {
            this.passwordExpirationEnabled = data.passwordSettings.expirationPolicy.enabled;
            this.userLockEnabled = data.userLockPolicy.enabled;
        });
    }

    revokeApiKey() {
        this.modal.confirm(`Are you sure you want to revoke API key for user '${this.username}'?`)
            .then(() => {
                this.userProfileDao.revokeApiKey({}, {username: this.username}).$promise.then(()=>{
                    this._getApiKeyState();
                });
            });
    }

    initActions(actionsController) {

        this.actionsController = actionsController;
        actionsController.setActionsDictionary(ACTIONS);
        actionsController.setActions([
            {
                name:'RevokeApiKey',
                visibleWhen: () => this.userdata && this.apiKeyExist && this.userdata.name !== 'anonymous',
                action: ()=>this.revokeApiKey()
            },
            {
                name:'UnlockUser',
                visibleWhen: () => this.userdata && this.userdata.locked && this.userdata.name !== 'anonymous',
                action: ()=>this.unlockUser()
            },
            {
                name:'ExpirePassword',
                visibleWhen: () => this.userdata && this.passwordExpirationEnabled && this.mode==='edit' && !this.userdata.credentialsExpired && (this.userdata.realm === 'internal' || !this.userdata.realm)  && this.userdata.name !== 'anonymous',
                action: ()=>this.expirePassword()
            },
            {
                name:'UnexpirePassword',
                visibleWhen: () => this.userdata && this.userdata.credentialsExpired && !this.userdata.locked  && this.userdata.name !== 'anonymous',
                action: ()=>this.unexpirePassword()
            },
            {
                name:'DeleteUser',
                visibleWhen: () => this.mode === 'edit' && this.userdata && this.userdata.name !== 'anonymous',
                action: ()=>this.deleteUser()
            }
        ]);

    }

}