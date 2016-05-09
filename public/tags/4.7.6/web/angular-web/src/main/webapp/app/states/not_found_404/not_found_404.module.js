function notFound404 ($stateProvider) {

    $stateProvider
        .state('not_found_404', {
            url: '/404',
            templateUrl: 'states/not_found_404/not_found_404.html',
            parent: 'app-layout',
        })
}

export default angular.module('not_found_404', [])
    .config(notFound404)