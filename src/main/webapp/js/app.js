var webplannerApp = angular.module('webplanner', [,'ui.bootstrap',
'ngRoute',
'webplanner.services',
'plannerControllers'
]);

webplannerApp.run(['$rootScope', '$q', '$modal', '$location',
  function($rootScope, $q, $modal, $location){
    
  }]);


webplannerApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: 'templates/main.html',
        controller: 'HomeCtrl'
      });
  }]);