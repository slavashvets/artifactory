export class AdminSecurityUserController {

    constructor($state, ArtifactoryModal, UserDao, $scope, GroupsDao, ArtifactoryGridFactory, uiGridConstants, AdminSecurityGeneralDao,
            commonGridColumns) {

        this.userDao = UserDao.getInstance();
        this.adminSecurityGeneralDao = AdminSecurityGeneralDao;
        this.uiGridConstants = uiGridConstants;
        this.groupsDao = GroupsDao.getInstance();
        this.modal = ArtifactoryModal;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.commonGridColumns = commonGridColumns;
        this.$scope = $scope;
        this.$state = $state;
        this._createGrid();
        this._initUsers();
    }

    _initUsers() {
        this.userDao.getAll().$promise.then((users)=> {
            //console.log(users);
            users.forEach((user)=>{
                user.permissions = _.pluck(user.permissionsList,'permissionName');
            });
            this.gridOption.setGridData(users);
        });
    }

    _createGrid() {
        this.gridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this.getColumns())
                .setMultiSelect()
                .setButtons(this._getActions())
                .setRowTemplate('default')
                .setBatchActions(this._getBatchActions());

        this.gridOption.isRowSelectable = (row) => {
            return row.entity.name !== 'anonymous';
        };
    }

    deleteUser(user) {
        let json = {userNames:[user.name]};
        this.modal.confirm(`Are you sure you want to delete user '${user.name}?'`)
            .then(() => this.userDao.delete(json).$promise.then(()=>this._initUsers()));
    }

    bulkDelete(){
        // Get All selected users
        let selectedRows = this.gridOption.api.selection.getSelectedRows();
        // Create an array of the selected users names
        let names = _.map(selectedRows, (user) => {return user.name;});
        // Create Json for the bulk request
        let json = {userNames: names};
        // console.log('Bulk delete....');
        // Ask for confirmation before delete and if confirmed then delete bulk of users
        this.modal.confirm(`Are you sure you want to delete ${names.length} users?`)
            .then(() => this.userDao.delete(json).$promise.then(() => this._initUsers()));
    }

    updateUser(user) {
        this.userDao.update(user).$promise.then(()=>this._initUsers());
    }

    checkExternalStatus(user) {
        this.userDao.checkExternalStatus(user).$promise.then((dataRes)=>{
//            console.log(dataRes);
            user.externalRealmStatus = dataRes.data.externalRealmStatus;
        });

    }

    getColumns() {
        return [
            {
                name: 'Name',
                displayName: 'Name',
                field: "name",
                cellTemplate: '<div class="ui-grid-cell-contents" ui-sref="^.users.edit({username: row.entity.name})"><a href="">{{row.entity.name}}</a></div>',
                sort: {
                    direction: this.uiGridConstants.ASC
                },
                width: '15%'
            },
            {
                name: 'Email',
                displayName: 'Email',
                field: "email",
                width: '15%'
            },
            {
                name: "Realm",
                displayName: "Realm",
                field: "realm",
                cellTemplate: '<div class="ui-grid-cell-contents">{{row.entity.realm}}'+
                              '<span ng-if="row.entity.externalRealmStatus"> | {{row.entity.externalRealmStatus}}</span>' +
                              '<span ng-if="!row.entity.externalRealmStatus && row.entity.externalRealmLink"> | <a href="" ng-click="grid.appScope.AdminSecurityUser.checkExternalStatus(row.entity)">{{row.entity.externalRealmLink}}</a></span></div>',
                width: '10%'
            },
            {
                field: "groups",
                name: "Related Groups",
                displayName: "Related Groups",
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.groups','row.entity.name'),
                width: '18%'
            },
            {
                field: "permissions",
                name: "Related Permissions",
                displayName: "Related Permissions",
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.permissions','row.entity.name'),
                width: '18%'
            },
            {
                name: "Admin",
                displayName: "Admin",
                field: "admin",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.admin'),
                width: '7%'
            },
            {
                name: "Lock",
                displayName: "Locked",
                field: "locked",
                cellTemplate: '<div class="ui-grid-cell-contents text-center"><a class="grid-column-button icon icon-lock" ng-if="row.entity.locked" cm-aditional-action="Unlock" ng-click="grid.appScope.AdminSecurityUser.unlockUsers(row.entity.name)" jf-tooltip="Unlock"></a></div>',
                width: '7%'
            },
            {
                field: "lastLoggedInMillis",
                name: "Last Login",
                displayName: "Last Login",
                cellTemplate: '<div class="ui-grid-cell-contents">{{row.entity.lastLoggedIn}}</div>',
                width: '10%'
            }
        ]
    }

    _getActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: (row) => this.deleteUser(row),
                visibleWhen: (row) => row.name != 'anonymous'
            }

        ];
    }

    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this.bulkDelete()
            },
            {
                icon: 'unlock',
                name: 'Unlock',
                callback: () => this.bulkUnlock(),
                disabledWhen: () => !this.hasSelectedUnlockedUsers()
            }
        ]
    }

    unlockUsers(users) {
        if (!_.isArray(users)) users = [users];

        this.adminSecurityGeneralDao.unlockUsers({},users).$promise.then((res)=>{
            if(res.status === 200) {
                this._initUsers()
            }
        });
    }

    hasSelectedUnlockedUsers() {
        let selectedRows = this.gridOption.api.selection.getSelectedRows();
        return _.findWhere(selectedRows,{locked: true});
    }

    bulkUnlock() {
        let selectedRows = this.gridOption.api.selection.getSelectedRows();
        let locked = _.filter(selectedRows,row=>row.locked);
        let names = _.pluck(locked,"name");
        this.unlockUsers(names);
    }

}