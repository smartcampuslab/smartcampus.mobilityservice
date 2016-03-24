var webplannerApp = angular.module('webplanner', [,'ui.bootstrap',
'ngRoute',
'webplanner.services',
'plannerControllers'
]);

webplannerApp.run(['$rootScope', '$q', '$modal', '$location', 'parking', 'bikesharing', 'taxi',
  function($rootScope, $q, $modal, $location, parking, bikesharing, taxi){
    $rootScope.EXTRAURBAN_AGENCIES = EXTRAURBAN_AGENCIES;
	$rootScope.CENTER = new google.maps.LatLng(CENTER[0],CENTER[1]);
    parking.init(PARKING_AGENCIES);
    bikesharing.init(BIKE_AGENCIES);
    taxi.init();

  }]);


webplannerApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: 'templates/main.html',
        controller: 'HomeCtrl'
      });
  }]);