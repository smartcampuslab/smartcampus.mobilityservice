var notification = angular.module('gameconsole', []);


notification.controller('GameCtrl', function($scope, $http) {
	$scope.users = [];
	$scope.userMap = {};
	$scope.selectedUser = null;
	$scope.selectedItinerary = null;
	$scope.selectedInstance = null;
	$scope.layers = [];

	var load = function() {
		$http.get("console/itinerary").then(function(data) {	
			var map = {};
			var users = [];
			$scope.counters = {};
			$scope.userTotals = {};
			data.data.forEach(function(descr) {
				if (!map[descr.userId]) {
					map[descr.userId] = [];
					users.push(descr.userId);
					$scope.userTotals[descr.userId] = {total:0, failed: 0};
				}
				map[descr.userId].push(descr);
				descr.instances.forEach(function(i) {
					$scope.userTotals[descr.userId].total++;
					if (!i.valid) {
						$scope.userTotals[descr.userId].failed++;
					}
				});
			});
			$scope.users = users;
			users.sort(function(a,b) {return parseInt(a) - parseInt(b);});
			$scope.userMap = map;
		});
	}
	load();
	
	$scope.selectUser = function(user) {
		if ($scope.selectedUser == user) $scope.selectedUser = null;
		else $scope.selectedUser = user;
		$scope.selectedItinerary = null;
		$scope.selectedInstance = null;
		resetLayers();
	}
	
	$scope.selectItinerary = function(itinerary) {
		resetLayers();
		$scope.selectedInstance = null;
		$scope.selectedItinerary = itinerary;
		itinerary.instances.sort(function(a,b) {
			if (!a.day && !b.day) return 0;
			if (!a.day) return -1;
			if (!b.day) return 1;
			return a.day.localeCompare(b.day);
		});
//		if (itinerary.instances.length == 1) {
//			$scope.selectInstance(itinerary.instances[0]);
//		}
	}

	var resetLayers = function() {
		if (!$scope.layers) return;
		
		$scope.layers.forEach(function(l) {
			l.setMap(null);
		});
		$scope.layers = [];
	}
    
	$scope.revalidate = function() {
		$http.post("console/validate",{}).then(function(data) {	
			load();
		});		
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
	       strokeColor: 'blue',
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
			    strokeColor: 'green',
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
	
	

