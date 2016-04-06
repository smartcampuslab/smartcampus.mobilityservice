var notification = angular.module('gameconsole', []);


notification.controller('GameCtrl', function($scope, $http) {
	$scope.users = [];
	$scope.userMap = {};
	$scope.selectedUser = null;
	$scope.selectedItinerary = null;
	$scope.selectedInstance = null;
	$scope.layers = [];

	$http.get("console/itinerary").then(function(data) {	
		var map = {};
		data.data.forEach(function(descr) {
			if (!map[descr.userId]) {
				map[descr.userId] = [];
				$scope.users.push(descr.userId);
			}
			map[descr.userId].push(descr);
		});
		$scope.userMap = map;
	});	
	
	$scope.selectUser = function(user) {
		if ($scope.selectedUser == user) $scope.selectedUser = null;
		else $scope.selectedUser = user;
		$scope.selectedItinerary = null;
		$scope.selectedInstance = null;
	}
	
	$scope.selectItinerary = function(itinerary) {
		$scope.selectedInstance = null;
		$scope.selectedItinerary = itinerary;
		if (itinerary.instances.length == 1) {
			$scope.selectInstance(itinerary.instances[0]);
		}
	}

	var resetLayers = function() {
		if (!$scope.layers) return;
		
		$scope.layers.forEach(function(l) {
			l.setMap(null);
		});
		$scope.layers = [];
	}
    
	$scope.selectInstance = function(instance) {
		$scope.selectedInstance = instance;
		
		resetLayers();

		// SHOW TRACKED DATA
		var coordinates = [];
		instance.geolocationEvents.sort(function(a,b){
			return a.recorded_at - b.recorded_at;
		});
        var bounds = new google.maps.LatLngBounds();
		instance.legs = [];
		var lastLeg = {activity_type: null};
		instance.geolocationEvents.forEach(function(e){
			var p = {lat:e.latitude,lng:e.longitude};
			coordinates.push(p);
        	bounds.extend(new google.maps.LatLng(p.lat,p.lng));
        	var type = e.activity_type;
        	if (type != lastLeg.activity_type && type != 'unknown') {
        		var leg = angular.copy(e);
        		leg.count = 1;
        		instance.legs.push(leg);
        		lastLeg = leg;
        	} else if (type == 'unknown') {
        		var leg = angular.copy(e);
        		leg.count = 1;
        		instance.legs.push(leg);
        	} else {
        		lastLeg.count++;
        		lastLeg.recorded_till = e.recorded_at;
        	}
		});
		
		
//		coordinates.splice(0,1);
//		coordinates.splice(coordinates.length-1,1);
    	$scope.map.fitBounds(bounds);
		
		newMarker(coordinates[0],'ic_start');
		newMarker(coordinates[coordinates.length-1],'ic_stop');
		
	    var path = new google.maps.Polyline({
	       path: coordinates,
	       geodesic: true,
	       strokeColor: '#FF0000',
	       strokeOpacity: 1.0,
	       strokeWeight: 2
	    });
	    $scope.layers.push(path);
	    path.setMap($scope.map);

	    // $SHOW PLANNED DATA
	    instance.itinerary.data.leg.forEach(function(leg) {
	    	var path = google.maps.geometry.encoding.decodePath(leg.legGeometery.points);
	    	var line = new google.maps.Polyline({
			    path: path,
			    strokeColor: '#00FF00',
			    strokeOpacity: 0.8,
			    strokeWeight: 2,
			    map: $scope.map
			  });
	    	newMarker(path[0],'step');
	    	$scope.layers.push(line);
	    });
	}

	var newMarker = function(pos, icon) {
		    var m = new google.maps.Marker({
	            position: pos,
	            icon: '../img/'+icon+'.png',
	            map: $scope.map,
	            draggable: false,
	            labelContent: "A",
	            labelAnchor: new google.maps.Point(3, 30),
	            labelClass: "labels"
	        });
		    $scope.layers.push(m);
		    return m;
	};

	
	$scope.initMap = function() {
	      if (!document.getElementById('map')) return;
	      var ll = null;
	      var mapOptions = null;
	      ll = {lat: 46.073769, lng: 11.125985};
		  mapOptions = {
		      zoom: 15,
		      center: ll
		  }   
	      $scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);
	}
	
	$scope.initMap();
})
	
	

