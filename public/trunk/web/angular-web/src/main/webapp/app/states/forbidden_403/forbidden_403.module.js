function forbidden403 ($stateProvider) {

    $stateProvider
        .state('forbidden_403', {
            url: '/403',
            templateUrl: 'states/forbidden_403/forbidden_403.html',
            parent: 'app-layout',
        })
}

export default angular.module('forbidden_403', [])
    .config(forbidden403)