var webplannerApp = angular.module('webplanner', [,'ui.bootstrap',
'ngRoute',
'webplanner.services',
'plannerControllers'
]);

webplannerApp.run(['$rootScope', '$q', '$modal', '$location', 'parking', 'bikesharing',
  function($rootScope, $q, $modal, $location, parking, bikesharing){
    $rootScope.CENTER = new google.maps.LatLng(46.071530, 11.119497);
    var agencies = ['COMUNE_DI_TRENTO','COMUNE_DI_ROVERETO'];
    parking.init(agencies);
    
    var bikeAgencies = ['trento','rovereto', 'pergine_valsugana'];
    bikesharing.init(bikeAgencies);

  }]);


webplannerApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: 'templates/main.html',
        controller: 'HomeCtrl'
      });
  }]);