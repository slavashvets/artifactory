import {LoginController} from './login.controller';

function loginCOnfig ($stateProvider) {
    $stateProvider

            .state('login', {
                url: '/login',
                templateUrl: 'states/login/login.html',
                controller: 'LoginController as Login',
                params: {oauthError: null},
                parent: 'login-layout'
            })
}

export default angular.module('changePassword', [])
        .config(loginCOnfig)
        .controller('LoginController', LoginController);