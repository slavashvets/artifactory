class jfSwitchToggleController {
    constructor($element, $transclude, $timeout) {
        $transclude(function(clone) {
            $timeout(function() {
                $element.find('label').prepend(clone);
            }, 0, false);
        });
    }
}

export function jfSwitchToggle() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            texton: '@?',
            textoff: '@?'
        },
        replace: true,
        controller: jfSwitchToggleController,
        templateUrl: 'directives/jf_switch_toggle/jf_switch_toggle.html'
    }
}