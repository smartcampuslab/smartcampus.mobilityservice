var plannerControllers = angular.module('plannerControllers', [])

.controller('HomeCtrl', ['$scope', '$routeParams', '$rootScope', '$modal', '$location', 'geocoder', 'planner', 'formatter',
  function($scope, $routeParams, $rootScope, $modal, $location, geocoder, planner, formatter) {

	// current user position, defaults to Trento
	$scope.myposition = new google.maps.LatLng(46.071530, 11.119497);
	
	$scope.mytime = new Date();
	$scope.mydate = new Date();
    $scope.fromMarker = null;
    $scope.toMarker = null;
    $scope.mode = 'fastest';
    $scope.means = {'TRANSIT':true};
	$scope.loading = false;
	$scope.currentItinerary = null;
	$scope.legElems = [];
	
	
	$scope.resetDrawings = function(){
		if ($scope.legElems) {
			$scope.legElems.forEach(function(e) {
				e.setMap(null);
			});
			$scope.legElems = [];
		}
	};
	
	// initialize map
    $scope.initMap = function() {
      if (!document.getElementById('map')) return;
      var ll = null;
      var mapOptions = null;
      ll = $scope.myposition;
	  mapOptions = {
	      zoom: 15,
	      center: ll
	  }   
      $scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);

	  $scope.updateAddress = function(obj, latLng) {
	    	geocoder.geocode(latLng.lat(),latLng.lng()).success(function(data){
	    		if (data && data.response && data.response.docs) {
	    			var point = data.response.docs[0];
	    			var ll = point.coordinate.split(',');
	    			obj.setPosition(new google.maps.LatLng(ll[0],ll[1]));
	    			var address = '';
	    			if (point.name) {
		    			address += point.name;
	    			}
	    			if (point.street && point.street != point.name) {
	    				if (address) address+=', ';
		    			address += point.street;
	    			}
	    			if (point.city) {
	    				if (address) address+=', ';
		    			address += point.city;
	    			}
	    			obj.address = address;
	    		}
	    	});
	  };

	  // create new marker with the specified icon and position,
	  // update address field of the marker object and set up drag listener
	  $scope.newMarker = function(pos, icon) {
	    var m = new google.maps.Marker({
            position: pos,
            icon: 'img/'+icon+'.png',
            map: $scope.map,
            draggable: true,
            labelContent: "A",
            labelAnchor: new google.maps.Point(3, 30),
            labelClass: "labels"
        });
	    $scope.updateAddress(m, pos);
	    
	    google.maps.event.addListener(m, 'dragend', function(evt) {
	    	$scope.updateAddress(m, evt.latLng);
	    	$scope.$apply();
	    });
	    return m;
	  };
	  
	  // add click listener to the map: first click creates 'from' marker
	  // second click creates 'to' marker, other clicks are ignored
	  google.maps.event.addListener($scope.map, 'click', function(evt) {
		  if ($scope.fromMarker == null) {
		    $scope.fromMarker = $scope.newMarker(evt.latLng, 'ic_start');
		  }
		  else if ($scope.toMarker == null) {
		    $scope.toMarker = $scope.newMarker(evt.latLng, 'ic_stop');
		  }
	  });
    }
    
    $scope.initMap();

    // set the 'from' field and the 'from' marker to the current position
    $scope.fromCurrent = function() {
    	if ($scope.myposition) {
    		if ($scope.fromMarker) $scope.fromMarker.setMap(null);
		    $scope.fromMarker = $scope.newMarker($scope.myposition, 'ic_start');
    	}
    };
    // set the 'to' field and the 'to' marker to the current position
    $scope.toCurrent = function() {
    	if ($scope.myposition) {
    		if ($scope.toMarker) $scope.toMarker.setMap(null);
		    $scope.toMarker = $scope.newMarker($scope.myposition, 'ic_stop');
    	}
    };

    // localize the user
    if(navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
          var pos = new google.maps.LatLng(position.coords.latitude,
                                           position.coords.longitude);

          $scope.myposition = pos;
          $scope.map.setCenter(pos);
        }, function() {
        });
      } else {
      }
    // for the date picker
    $scope.today = function() {
    	$scope.mydate = new Date();
    };
    $scope.today();

    // for the date picker
    $scope.clear = function () {
        $scope.mydate = null;
    };
    // for the date picker
    $scope.open = function($event) {
      $event.preventDefault();
      $event.stopPropagation();

      $scope.opened = true;
    };
    
    // time picker updates time value
    $scope.changed = function () {
        console.log('Time changed to: ' + $scope.mytime);
    };
    
    // clear the 'from' and 'to' markers
    $scope.reset = function() {
    	$scope.currentItinerary = null;
    	$scope.resetDrawings();
    	if (!!$scope.fromMarker) {
    		$scope.fromMarker.setMap(null);
    		$scope.fromMarker = null;
    	}
    	if (!!$scope.toMarker) {
    		$scope.toMarker.setMap(null);
    		$scope.toMarker = null;
    	}
    	$scope.plans = null;
    }
    
    var convertMeans = function() {
    	res = [];
    	if ($scope.means['TRANSIT']) res.push('TRANSIT');
    	if ($scope.means['CAR']) res.push('CAR,CARWITHPARKING');
    	if ($scope.means['WALK']) res.push('WALK');
    	if ($scope.means['BIKE']) res.push('BICYCLE,SHAREDBIKE,SHAREDBIKE_WITHOUT_STATION');
    	return res.join(',');
    };
    
    // plan route
    $scope.plan = function() {
    	$scope.currentItinerary = null;
    	$scope.resetDrawings();
    	if (!$scope.fromMarker || !$scope.toMarker) {
    		$scope.errorMsg = 'Specify from/to locations!';
    		return;
    	}
    	$scope.loading = true;
    	planner.plan(
    			$scope.fromMarker.getPosition(), 
    			$scope.toMarker.getPosition(),
    			convertMeans(),
    			$scope.mode,
    			$scope.mydate,
    			$scope.mytime
    			)
    	.success(function(data){
    		if (data && data.length > 0) {
    			data.sort(function(a,b) {
    				if (a.promoted != b.promoted) {
    					return b.promoted - a.promoted;
    				}
    				return a.startime != b.startime ? a.startime - b.startime : a.duration - b.duration;
    			});
    			
    			data.forEach(function(it, idx){
    				it.means = formatter.extractItineraryMeans(it);
    				it.index = idx;
    			});
    			
    			$scope.plans = data;
        		$scope.errorMsg = null;
    		} else {
        		$scope.errorMsg = 'No results found';
    		}
    		$scope.loading = false;
    	})
    	.error(function(data){
    		$scope.errorMsg = 'Error planning the route';
    		$scope.loading = false;
    	});
    }
    
    $scope.toTime = function(millis) {
    	return formatter.getTimeStr(new Date(millis));
    };

    $scope.showPlan = function(plan) {
    	$scope.currentItinerary = plan;
    	$scope.resetDrawings();
    	
    	for (var i = 0; i < plan.leg.length; i++) {
    		var line = new google.maps.Polyline({
    		    path: google.maps.geometry.encoding.decodePath(plan.leg[i].legGeometery.points),
    		    strokeColor: "#FF0000",
    		    strokeOpacity: 0.8,
    		    strokeWeight: 2,
    		    map: $scope.map
    		  });
    		$scope.legElems.push(line);
    	}
    }
    
}]);
