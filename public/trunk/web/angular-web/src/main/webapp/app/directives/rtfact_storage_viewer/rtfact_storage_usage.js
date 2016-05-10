import SpecialValues from './special_values';

class rtfactStorageUsageController {
    constructor() {
        this.SpecialValues = SpecialValues;
    }

    getPercents() {
        if (!SpecialValues.isSpecialValue(this.total)) return ((this.used / this.total) * 100) + '%';
        else return 'calc(100% - 40px)'
    }
}

export function rtfactStorageUsage() {
    return {
        restrict: 'E',
        scope: {
            total: '=',
            used: '=',
            thresholds: '='
        },
        controller: rtfactStorageUsageController,
        controllerAs: 'StorageUsage',
        templateUrl: 'directives/rtfact_storage_viewer/rtfact_storage_usage.html',
        bindToController: true
    };
}
