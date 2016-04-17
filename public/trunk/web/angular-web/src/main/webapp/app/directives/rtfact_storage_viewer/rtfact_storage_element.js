import SpecialValues from './special_values';
import STORAGE_TYPES_DICTIONARY from './storage_types_dictionary.js';


class rtfactStorageElementController {
    constructor($element, $timeout, $filter) {
        this.SpecialValues = SpecialValues;
        this.$element = $element;
        this.fileSizeFilter = $filter('filesize');
        this.selectedSub = null;
        this.$timeout = $timeout;
        this.MINIMUM_SUB_WIDTH = 10; //percent
        this.DEFAULT_UNKNOWN_WIDTH = 12; //percent
        this.STORAGE_TYPES_DICTIONARY = STORAGE_TYPES_DICTIONARY;

        if (this.above) {
            this.above.below = this;
        }

        if (this.element.subElements && this.element.subElements.length) {
            this.calcSubWidths();
        }

        this.thresholds = this.getThresholds();

    }

    getSubWidth(sub) {
        if (sub.displayWidth) return sub.displayWidth+'%';
        else {
            this.minimumSubWidth = this.MINIMUM_SUB_WIDTH;
            this.unknownWidth = this.DEFAULT_UNKNOWN_WIDTH;
            this.calcSubWidths();
            return sub.displayWidth+'%';
        }
    }

    calcSubWidths() {
       let totalSubsSpace = 0;
        this.element.subElements.forEach((sub) => {
            if(!SpecialValues.isSpecialValue(sub.data.usageSpace)) {
                totalSubsSpace += parseFloat(!SpecialValues.isSpecialValue(sub.data.totalSpace) ? sub.data.totalSpace : sub.data.usageSpace);
            }
        });

        let totalPercents = 0;
        let totalPercentsNatural = 0;
        let belowMinimum = 0;
        let unknowns = 0;
        this.element.subElements.forEach((sub) => {
            let width;
            let natural = true;
            if (SpecialValues.isSpecialValue(sub.data.usageSpace)) {
                width = this.unknownWidth;
                unknowns++;
                natural = false;
                sub.unknown = true;
            }
            else {
                width = (parseFloat(!SpecialValues.isSpecialValue(sub.data.totalSpace) ? sub.data.totalSpace : sub.data.usageSpace) / totalSubsSpace)*100;
                if (width < this.minimumSubWidth) {
                    width = this.minimumSubWidth;
                    belowMinimum++;
                    natural = false;
                }
            }

            if (natural) totalPercentsNatural += width;

            sub.displayWidth = width;
            totalPercents += width;
        });

        //normalize
        while ((totalPercents > 100.0001 || totalPercents < 99) && totalPercentsNatural > 0) {
//            console.log('!!');
            let minimumWidth = this.minimumSubWidth * belowMinimum + this.unknownWidth * unknowns;
            let remainingWidth = 100 - minimumWidth;
            let newTotal = 0;
            let newTotalNatural = 0;
            this.element.subElements.forEach((sub) => {
                if (sub.displayWidth === this.minimumSubWidth || sub.unknown) {
                    newTotal += sub.displayWidth;
                }
                else {
                    let normalWidth = (sub.displayWidth/totalPercentsNatural)*remainingWidth;
                    if (normalWidth < this.minimumSubWidth) {
                        normalWidth = this.minimumSubWidth;
                        belowMinimum++;
                    }
                    else newTotalNatural += normalWidth;
                    sub.displayWidth = normalWidth;
                    newTotal += normalWidth;
                }
            });
            totalPercents = newTotal;
            totalPercentsNatural = newTotalNatural;
        }

        if (!totalPercentsNatural && totalPercents > 100) {
            this.minimumSubWidth *= 100/totalPercents;
            this.calcSubWidths();
        }

        if (totalPercents < 100 && totalPercentsNatural === 0 && unknowns === this.element.subElements.length) {
            this.unknownWidth = 100/unknowns;
            this.calcSubWidths();
        }

    }

    onClick(e) {
        e.stopImmediatePropagation();
        e.preventDefault();

        if (this.level === 'sub') {
            this.parent.onChildClicked(this);
        }
    }

    onChildClicked(child) {
        if (this.below) this.below.onChildClicked(null);
        this.selectedSub = this.selectedSub === child ? null : child;

        //for debug
        if (child && localStorage._debugStorageViewer === 'true' && window.storageRef) window.storageRef = child.element;
    }

    isTextOverflowing() {
        if (this.level !== 'sub') return false;
        let textContent = $(this.$element).find('.text-content');
        return textContent[0].scrollWidth > textContent.innerWidth();
    }

    getUsageString(includePercentage = false) {
        if (this.element.data.usageSpace === SpecialValues.UNSUPPORTED_VALUE) return null;
        else {
            let usageString = (this.element.data.usageSpace !== '0' ?
                this.fileSizeFilter(this.element.data.usageSpace) + ' / ' : (includePercentage ? '0 / ' : 'Empty - ')) +
                (!SpecialValues.isSpecialValue(this.element.data.totalSpace) ?
                    this.fileSizeFilter(this.element.data.totalSpace) :
                    (this.element.data.totalSpace === SpecialValues.INFINITY_VALUE ? '\u221E' : '?'));

            if (includePercentage) {
                usageString += (!SpecialValues.isSpecialValue(this.element.data.totalSpace) ?
                ' (' + (100*this.element.data.usageSpace/this.element.data.totalSpace).toFixed(1) + '%)' : '');
            }
            return usageString;
        }
    }

    getThresholds() {
        let thresholds = [];

        if (this.element.caches && this.element.caches[0] && this.element.caches[0].data.quotaErrorLimit) {
            this.element.data.quotaErrorLimit = this.element.caches[0].data.quotaErrorLimit;
            delete this.element.caches[0].data.quotaErrorLimit;
        }
        if (this.element.caches && this.element.caches[0] && this.element.caches[0].data.quotaWarningLimit) {
            this.element.data.quotaWarningLimit = this.element.caches[0].data.quotaWarningLimit;
            delete this.element.caches[0].data.quotaWarningLimit;
        }

        if (this.element.data && this.element.data.quotaErrorLimit) {
            thresholds.push({
                value: this.element.data.quotaErrorLimit,
                type: 'error'
            })
        }
        if (this.element.data && this.element.data.quotaWarningLimit) {
            thresholds.push({
                value: this.element.data.quotaWarningLimit,
                type: 'warning'
            })
        }

        return thresholds;
    }

}

export function rtfactStorageElement(recursiveDirective) {
    return {
        restrict: 'E',
        scope: {
            level: '@', //'top' / 'sub'
            element: '=',
            parent: '=',
            above: '='
        },
        controller: rtfactStorageElementController,
        controllerAs: 'StorageElement',
        templateUrl: 'directives/rtfact_storage_viewer/rtfact_storage_element.html',
        bindToController: true,
        compile: (element) => {
            return recursiveDirective.compile(element);
        }
    };
}
