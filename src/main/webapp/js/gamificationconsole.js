var gamificationConsole = angular.module('gamificationConsole', [,'ui.bootstrap',
'ngRoute',
'gameconsole'
]);

gamificationConsole.config(['$routeProvider',
                     function($routeProvider) {
                       $routeProvider.
                         when('/', {
                           templateUrl: '../templates/gamificationconsoleinner.html',
                           controller: 'GameCtrl'
                         }).
                       when('/users', {
                           templateUrl: '../templates/gamificationconsoleusers.html',
                           controller: 'UsersCtrl'
                         }).
                         when('/checkin', {
                             templateUrl: '../templates/gamificationconsolecheckin.html',
                             controller: 'CheckinCtrl'
                           });
                     }]);

gamificationConsole.run(['$rootScope', '$q',
  function($rootScope, $q){
}]);


