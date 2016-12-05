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
                         });
                     }]);

gamificationConsole.run(['$rootScope', '$q',
  function($rootScope, $q){
  }]);


