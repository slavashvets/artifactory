export function jfListMaker() {

    return {
        restrict: 'E',
        scope: {
            values: '=',
            label: '@',
            helpTooltip: '=',
            objectName: '@',
            ngDisabled: '=',
            noSort: '=?',
            minLength: '@'
        },
        templateUrl: 'directives/jf_list_maker/jf_list_maker.html',
        controller: jfListMakerController,
        controllerAs: 'jfListMaker',
        bindToController: true
    }
}

/**
 * API for the jfDragDrop directive
 */
class jfListMakerController {

    constructor($attrs) {
        this.noSort = this.noSort || $attrs.hasOwnProperty('noSort');
        if (this.values && !this.noSort) this.values = _.sortBy(this.values);
        this.minLength = this.minLength || 0;
    }
    addValue() {
        if (!this.values) this.values = [];

        this.errorMessage = null;

        if (_.isEmpty(this.newValue)) {
            this.errorMessage = "Must input value";
        }
        else if (!this._isValueUnique(this.newValue)) {
            this.errorMessage = "Value already exists";
        }
        else {
            this.values.push(this.newValue);
            this.newValue = null;
        }
        if (!this.noSort) {
            this.values = _.sortBy(this.values);
        }
    }

    removeValue(index) {
        this.values.splice(index,1);
    }

    _isValueUnique(text) {
        return !this.values || this.values.indexOf(text) == -1;
    }
}