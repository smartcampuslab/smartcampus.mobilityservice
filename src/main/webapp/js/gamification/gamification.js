var notification = angular.module('gameconsole', [ 'ngScrollable' ]);

notification.controller('GameCtrl', function($scope, $http) {
	$scope.users = [];
	$scope.userMap = {};
	$scope.selectedUser = null;
	$scope.selectedItinerary = null;
	$scope.selectedInstance = null;
	$scope.layers = [];
	$scope.fixpaths = false;

//	$scope.init = function() {
//		$http.get("console/appId").success(function(data) {	
//			$scope.appId = data;
//		});
//	}
//	
//	$scope.init();
	
	var load = function() {
		$http.get("console/appId").success(function(data) {	
			$scope.appId = data;
			$http.get("console/users", {"headers" : { "appId" : $scope.appId}}).then(function(data) {
				var users = [];
				$scope.userTotals = {};
				data.data.forEach(function(descr) {
					users.push(descr.userId);
					$scope.userTotals[descr.userId] = {
						"total" : descr.total,
						"failed" : (descr.total - descr.valid)
					};
				});

				$scope.users = users;
				$scope.userMap = {};
			});			
		});
	}

	load();

	$scope.selectUser = function(user) {
		if ($scope.selectedUser == user)
			$scope.selectedUser = null;
		else {
			$scope.selectedUser = user;

			if (!$scope.userMap[user]) {
				$http.get("console/useritinerary/" + user, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
					$scope.userMap[user] = data.data;
				});
			}

		}
		$scope.selectedItinerary = null;
		$scope.selectedInstance = null;
		resetLayers();
	}

	$scope.selectItinerary = function(itinerary) {
		resetLayers();
		$scope.selectedInstance = null;
		$scope.selectedItinerary = itinerary;
//		itinerary.instances.sort(function(a, b) {
//			if (!a.day && !b.day)
//				return 0;
//			if (!a.day)
//				return -1;
//			if (!b.day)
//				return 1;
//			return a.day.localeCompare(b.day);
//		});
		
		// if (itinerary.instances.length == 1) {
		// $scope.selectInstance(itinerary.instances[0]);
		// }
	}

	$scope.validColor = function(totals) {
		var r = 127 + Math.floor(128 * Math.pow(totals.failed / totals.total, 1.5));
		var g = 0 + Math.floor(255 * ((totals.total - totals.failed) / totals.total));
		return "color:rgb(" + r + "," + g + "," + 64 + ")";
	}

	var resetLayers = function() {
		if (!$scope.layers)
			return;

		$scope.layers.forEach(function(l) {
			l.setMap(null);
		});
		$scope.layers = [];
	}

	$scope.revalidate = function() {
		$http.post("console/validate", {}, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			load();
		});
	}

	$scope.reselectInstance = function() {
		if ($scope.selectedInstance != null) {
			$scope.selectInstance($scope.selectedInstance);
		}
	}
	
	$scope.selectInstance = function(instance) {
		$scope.selectedInstance = instance;

		resetLayers();

		// SHOW TRACKED DATA
		var coordinates = [];
		instance.geolocationEvents.sort(function(a, b) {
			return a.recorded_at - b.recorded_at;
		});
		var bounds = new google.maps.LatLngBounds();
		instance.legs = [];
		var lastLeg = {
			activity_type : null
		};
		instance.geolocationEvents.forEach(function(e) {
			var p = {
				lat : e.latitude,
				lng : e.longitude,
				acc: e.accuracy
			};
			coordinates.push(p);
			bounds.extend(new google.maps.LatLng(p.lat, p.lng));
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

		// coordinates.splice(0,1);
		// coordinates.splice(coordinates.length-1,1);
		$scope.map.fitBounds(bounds);

		if ($scope.fixpaths) {
			coordinates = transform(coordinates);
		}		
		
		newMarker(coordinates[0], 'ic_start');
		newMarker(coordinates[coordinates.length - 1], 'ic_stop');


		var path = new google.maps.Polyline({
			path : coordinates,
			geodesic : true,
			strokeColor : 'blue',
			strokeOpacity : 1.0,
			strokeWeight : 2
		});
		$scope.layers.push(path);
		path.setMap($scope.map);

		// $SHOW PLANNED DATA
		if (instance.itinerary != null) {
			instance.itinerary.data.leg.forEach(function(leg) {
				var path = google.maps.geometry.encoding.decodePath(leg.legGeometery.points);
				var line = new google.maps.Polyline({
					path : path,
					strokeColor : 'green',
					strokeOpacity : 0.8,
					strokeWeight : 2,
					map : $scope.map
				});
				newMarker(path[0], 'step');
				$scope.layers.push(line);
			});
		}
	}

	var newMarker = function(pos, icon) {
		var m = new google.maps.Marker({
			position : pos,
			icon : '../img/' + icon + '.png',
			map : $scope.map,
			draggable : false,
			labelContent : "A",
			labelAnchor : new google.maps.Point(3, 30),
			labelClass : "labels"
		});
		$scope.layers.push(m);
		return m;
	};
	
	
    if (typeof (Number.prototype.toRad) === "undefined") {
        Number.prototype.toRad = function () {
            return this * Math.PI / 180;
        }
    }
	
	function dist(p1, p2) {
        var pt1 = [p1.lat, p1.lng];
        var pt2 = [p2.lat, p2.lng];
    
        var d = false;
        if (pt1 && pt1[0] && pt1[1] && pt2 && pt2[0] && pt2[1]) {
            var lat1 = Number(pt1[0]);
            var lon1 = Number(pt1[1]);
            var lat2 = Number(pt2[0]);
            var lon2 = Number(pt2[1]);

            var R = 6371; // km
            //var R = 3958.76; // miles
            var dLat = (lat2 - lat1).toRad();
            var dLon = (lon2 - lon1).toRad();
            var lat1 = lat1.toRad();
            var lat2 = lat2.toRad();
            var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            d = R * c;
        } else {
            console.log('cannot calculate distance!');
        }
        return d;
  }
  
  function transform(array) {
    var res = [];
    for (var i = 1; i < array.length; i++) {
      transformPair(array[i-1], array[i], res, dist);
    }
    return res;
  }
  
  function compute(v1, a1, v2, a2, d) {
    if ((a1 + a2)/1000 > d) {
      var v = a1 > a2 ? (v2 - (v2-v1)*a2/a1) : (v1+ (v2-v1)*a1/a2);
      return [v,v];   
    }
    return [v1 + (v2-v1)*a1/d/1000, v2 - (v2-v1)*a2/d/1000];
  }
  
  function computeLats(p1, p2, d) {
    if (p1.lat > p2.lat) {
      var res = computeLats(p2,p1, d);
      return [res[1],res[0]];
    }
    return compute(p1.lat, p1.acc, p2.lat, p2.acc, d);
  }
  function computeLngs(p1, p2, d) {
    if (p1.lng > p2.lng) {
      var res = computeLngs(p2, p1, d);
      return [res[1],res[0]];
    }
    return compute(p1.lng, p1.acc, p2.lng, p2.acc, d);
  }
  
  function transformPair(p1, p2, res, distFunc) {
    var d = distFunc(p1,p2);
	if (d != 0) {
		var lats = computeLats(p1,p2,d);
		var lngs = computeLngs(p1,p2,d);
		res.push({lat: lats[0], lng: lngs[0], acc: (p1.acc + p2.acc) / 2});
		res.push({lat: lats[1], lng: lngs[1], acc: (p1.acc + p2.acc) / 2});
	}
  }	
	
	
	
	
	
	
	

	$scope.initMap = function() {
		document.getElementById("left-scrollable").style.height = (window.innerHeight - 185) + "px";
		document.getElementById("right-scrollable").style.height = (window.innerHeight / 2 - 60) + "px";
		if (!document.getElementById('map'))
			return;
		var ll = null;
		var mapOptions = null;
		ll = {
			lat : 46.073769,
			lng : 11.125985
		};
		mapOptions = {
			zoom : 15,
			center : ll
		}
		$scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);
	}

	$scope.initMap();
}).directive('toggle', function() {
	return {
		span : function(scope, element, attrs) {
			if (attrs.toggle == "tooltip") {
				$(element).tooltip();
			}
			if (attrs.toggle == "popover") {
				$(element).popover();
			}
		}
	};
})
