import TOOLTIP from '../../../../constants/artifact_tooltip.constant';
import FIELD_OPTIONS from '../../../../constants/field_options.constats';

export class AdminAdvancedStorageSummaryController {
    constructor($scope,$timeout,StorageSummaryDao, ArtifactoryGridFactory, uiGridConstants, commonGridColumns,$compile, ArtifactoryFeatures) {
        this.$scope = $scope;
        this.$compile = $compile;
        this.$timeout = $timeout;
        this.commonGridColumns = commonGridColumns;
        this.storageSummary = {};
        this.gridOption = {};
        this.uiGridConstants = uiGridConstants;
        this.storageSummaryDao = StorageSummaryDao.getInstance();
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.features = ArtifactoryFeatures;
        this.TOOLTIP = TOOLTIP.admin.advanced.storageSummary;
        this.counterTooltip = 'List includes all Local and Virtual repositories, and Remote repositories configured to store artifacts locally.'
        this.binariesKeys = ['binariesSize','binariesCount','artifactsSize','artifactsCount','optimization','itemsCount'];
        this.storageSummaryDao.get().$promise.then((result) => {
            this.storageSummary = result;
            this.storageSummary.repositoriesSummaryList = _.map(this.storageSummary.repositoriesSummaryList,(row)=>{

                if (row.repoKey==='TOTAL') {
                    row.percentage = 100;
                    row.percentageDisplay = '100%';
                }
                else {
                    row.percentage = !_.isNaN(parseFloat(row.percentage)) ? parseFloat(row.percentage) : row.percentage;
                    row.percentageDisplay = _.isNumber(row.percentage) ? row.percentage + '%' : 'N/A';
                }

                if (row.repoType === 'NA') row.repoType = 'N/A';
                if (row.packageType === 'NA') row.packageType = 'N/A';

                if (row.repoKey === 'TOTAL' || row.repoKey === 'auto-trashcan') {
                    row['__doNotCount__'] = true;
                    row.packageType = 'N/A';
                    row._specialRow = true;
                }

                if (row.repoKey === 'auto-trashcan') {
                    row.trashcan = true;
                    row.packageType = 'Trash';
                    row.repoKey = "Trash Can";
                }

                let repoKey = row.repoKey;
                for (let key in row) {
                    if (key !== '__doNotCount__' && key !== 'percentageDisplay') row[key] = {value: row[key], repoKey: repoKey};
                }

                var rowPackageType =_.find(FIELD_OPTIONS.repoPackageTypes, (type) => {
                    return type.serverEnumName == row.packageType.value;
                });
                if (rowPackageType)
                    row.typeIcon = rowPackageType.icon;

                if (row.packageType.value === 'Trash')
                    row.typeIcon = 'trash';
                
                return row;
            });

            //This is for assuring that even without sorting, total will always be first and trash will be second
            let total = _.findWhere(this.storageSummary.repositoriesSummaryList,{repoKey: {value:'TOTAL'}});
            let trash = _.findWhere(this.storageSummary.repositoriesSummaryList,{repoKey: {value:'Trash Can'}});
            let totalIndex = this.storageSummary.repositoriesSummaryList.indexOf(total);
            let trashIndex = this.storageSummary.repositoriesSummaryList.indexOf(trash);
            this.storageSummary.repositoriesSummaryList.splice(totalIndex,1);
            this.storageSummary.repositoriesSummaryList.splice(trashIndex,1);
            this.storageSummary.repositoriesSummaryList.unshift(trash);
            this.storageSummary.repositoriesSummaryList.unshift(total);

            if (this.storageSummary.fileStoreSummary && this.storageSummary.fileStoreSummary.storageDirectory.indexOf(', ') != -1) {
                this.storageSummary.fileStoreSummary.storageDirectory = '<div class="storage-multiple-mounts">' + this.storageSummary.fileStoreSummary.storageDirectory.replace(/, /g, '<br>') + '</div>';
                this.storageSummary.fileStoreSummary.storageType = 'Advanced Configuration';
            }
            
            this.createGrid();
        });
    }

    createGrid() {
        this.gridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this.getColumns())
                .setGridData(this.storageSummary.repositoriesSummaryList)
                .setRowTemplate('default');

        this.gridOption.afterRegister((gridApi)=>{
            gridApi.pagination.on.paginationChanged(this.$scope, (pageNumber, pageSize) => {
                let specialsToRemove = $('.ui-grid-row.special-row');
                specialsToRemove.removeClass('special-row');
                this.$timeout(()=>{
                    let specials = $('.special-row');
                    specials.parent().parent().addClass('special-row');
                    specials.removeClass('special-row')
                },100)
            });
        });

        this.$timeout(()=>{
            let counterElem = $('.grid-counter');
            let tooltipElem = $('<jf-help-tooltip html="StorageSummaryController.counterTooltip"></jf-help-tooltip>');
            counterElem.append(tooltipElem);
            this.$compile(tooltipElem)(this.$scope);

            let specials = $('.special-row');
            specials.parent().parent().addClass('special-row');
            specials.removeClass('special-row')

        })
    }

    sortGeneral(a,b) {
        let dir = 'asc';
        if (this) {
            dir = _.findWhere(this.ctrl.gridOption.api.grid.columns, {field: this.column}).sort.direction;
        }
        if (a.repoKey === 'TOTAL') return dir === 'desc' ? 1 : -1;
        else if (b.repoKey === 'TOTAL') return dir === 'desc' ? -1 : 1;
        else if (a.repoKey === 'Trash Can') return dir === 'desc' ? 1 : -1;
        else if (b.repoKey === 'Trash Can') return dir === 'desc' ? -1 : 1;
        else return a.value > b.value ? 1 : a.value < b.value ? -1 : 0;
    }

    sortByteSizes(a,b) {
        let dir = 'asc';
        if (this) {
            dir = _.findWhere(this.ctrl.gridOption.api.grid.columns, {field: this.column}).sort.direction;
        }

        let res = 0;
        if (a===undefined || b===undefined) return res;

        if (a.repoKey === 'TOTAL') return dir === 'desc' ? 1 : -1;
        else if (b.repoKey === 'TOTAL') return dir === 'desc' ? -1 : 1;
        else if (a.repoKey === 'Trash Can') return dir === 'desc' ? 1 : -1;
        else if (b.repoKey === 'Trash Can') return dir === 'desc' ? -1 : 1;
        else {
            var tb = [a.value.match('TB'), b.value.match('TB')],
                gb = [a.value.match('GB'), b.value.match('GB')],
                mb = [a.value.match('MB'), b.value.match('MB')],
                kb = [a.value.match('KB'), b.value.match('KB')]

            res = (tb[0] && !tb[1]) ? 1 : (tb[1] && !tb[0]) ? -1 :
                      (gb[0] && !gb[1]) ? 1 : (gb[1] && !gb[0]) ? -1 :
                      (mb[0] && !mb[1]) ? 1 : (mb[1] && !mb[0]) ? -1 :
                      (kb[0] && !kb[1]) ? 1 : (kb[1] && !kb[0]) ? -1 :
                      (parseFloat(a.value.match(/[+-]?\d+(\.\d+)?/)[0]) > parseFloat(b.value.match(/[+-]?\d+(\.\d+)?/)[0])) ? 1 : -1
        }

        return res;
    }

    getColumns() {
        return [
            {
                field: "repoKey",
                name: "Repository Key",
                sortingAlgorithm : this.sortGeneral.bind({column: 'repoKey', ctrl: this}),
                cellTemplate: '<div class="ui-grid-cell-contents" id="repoKey">{{row.entity.repoKey.value}}</div>',
                displayName: "Repository Key"
            },
            {
                field: "repoType",
                name: "Repository Type",
                sortingAlgorithm : this.sortGeneral.bind({column: 'repoType', ctrl: this}),
                cellTemplate: '<div class="ui-grid-cell-contents" id="repoType">{{row.entity.repoType.value}}</div>',
                displayName: "Repository Type"
            },
            {
                field: "packageType",
                name: "Package Type",
                displayName: "Package Type",
                sortingAlgorithm : this.sortGeneral.bind({column: 'packageType', ctrl: this}),
                cellTemplate: this.commonGridColumns.iconColumn('row.entity.packageType.value', 'row.entity.typeIcon', 'repo-type-icon')
            },
            {
                field: "percentage",
                cellTemplate: '<div class="ui-grid-cell-contents text-center" id="storage-precentage" >{{row.entity.percentageDisplay}}</div>',
                name: "Percentage",
                sortingAlgorithm : this.sortGeneral.bind({column: 'percentage', ctrl: this}),
                displayName: "Percentage"
            },
            {
                field: "usedSpace",
                name: "Used Space",
                displayName: "Artifacts Size",
                cellTemplate: '<div class="ui-grid-cell-contents text-center" id="used-space" >{{row.entity.usedSpace.value}}</div>',
                sortingAlgorithm : this.sortByteSizes.bind({column: 'usedSpace', ctrl: this}),
                sort: {
                    direction: this.uiGridConstants.DESC
                }
            },
            {
                field: "filesCount",
                name: "Files",
                sortingAlgorithm : this.sortGeneral.bind({column: 'filesCount', ctrl: this}),
                cellTemplate: '<div class="ui-grid-cell-contents" id="files" >  {{row.entity.filesCount.value}}</div>',
                displayName: "Files"
            },
            {
                field: "foldersCount",
                name: "Folders",
                sortingAlgorithm : this.sortGeneral.bind({column: 'foldersCount', ctrl: this}),
                cellTemplate: '<div class="ui-grid-cell-contents" id="folders" >{{row.entity.foldersCount.value}}</div>',
                displayName: "Folders"
            },
            {
                field: "itemsCount",
                name: "Items",
                sortingAlgorithm : this.sortGeneral.bind({column: 'itemsCount', ctrl: this}),
                cellTemplate: '<div class="ui-grid-cell-contents" id="items" >{{row.entity.itemsCount.value}}</div>',
                displayName: "Items"
            }

        ]
    }


}