var plannerControllers = angular.module('plannerControllers', [])

.controller('HomeCtrl', ['$scope', '$routeParams', '$rootScope', '$modal', '$location', 'geocoder', 'planner', 'formatter', 'parking', 'bikesharing',
  function($scope, $routeParams, $rootScope, $modal, $location, geocoder, planner, formatter, parking, bikesharing) {

	// current user position, defaults to Trento
	$scope.myposition = $rootScope.CENTER;
	
	$scope.mytime = new Date();
	$scope.mydate = new Date();
    $scope.fromMarker = null;
    $scope.toMarker = null;
    $scope.mode = 'fastest';
    
    $scope.useCoordinates = false;
    
    $scope.means = {'TRANSIT':true};
    $scope.layers = {'PARKING':{show: false, elements: null, get: parking.getAll},'BIKESHARING':{show: false, elements: null, get: bikesharing.getAll}};
	$scope.currentItinerary = null;
	$scope.legElems = [];
	$scope.planned = false;
	$scope.loadingInstance = null;
	
	$scope.resetDrawings = function(){
		if ($scope.legElems) {
			$scope.legElems.forEach(function(e) {
				e.setMap(null);
			});
			$scope.legElems = [];
		}
//		for (var l in $scope.layers) {
//			$scope.layers[l].show = false;
//			if ($scope.layers[l].elements) {
//				$scope.layers[l].elements.forEach(function(e) {
//					e.setMap(null);
//				});
//			}
//		}
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

	  $scope.updateAddress = function(obj, latLng, replan, keepAddress) {
		  if ($scope.useCoordinates) {
			  obj.setPosition(latLng);
			  if (!keepAddress) {
				  obj.address = latLng.lat()+','+latLng.lng();
			  }
	          if (replan && $scope.planned) {
				$scope.plan();
			  }
		  } else {
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
	    			if (replan && $scope.planned) {
	    				$scope.plan();
	    			}
	    		}
	    	});
		  }
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
	    	$scope.updateAddress(m, evt.latLng, true);
	    	$scope.$apply();
	    });
	    return m;
	  };
	  
	  // add click listener to the map: first click creates 'from' marker
	  // second click creates 'to' marker, other clicks are ignored
	  google.maps.event.addListener($scope.map, 'click', function(evt) {
		  if ($scope.fromMarker == null) {
		    $scope.fromMarker = $scope.newMarker(evt.latLng, 'ic_start');
	    	$scope.$apply();
		  }
		  else if ($scope.toMarker == null) {
		    $scope.toMarker = $scope.newMarker(evt.latLng, 'ic_stop');
	    	$scope.$apply();
		  }
	  });
    }
    
    $scope.changeAddressModality = function() {
    	$scope.useCoordinates = !$scope.useCoordinates;
    	$scope.reset();
    };

    $scope.changeAddress = function(m) {
    	var coords = m.address.split(',');
    	var ll = new google.maps.LatLng(new Number(coords[0]),new Number(coords[1]));
    	$scope.updateAddress(m,ll,false, true);
    	$scope.$apply();
    };
    
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
        console.log('Ora cambiata in: ' + $scope.mytime);
    };
    
    // clear the 'from' and 'to' markers
    $scope.reset = function() {
    	$scope.planned = false;
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
    	if ($scope.means['SHAREDCAR']) res.push('SHAREDCAR_WITHOUT_STATION');
    	if ($scope.means['WALK']) res.push('WALK');
    	if ($scope.means['BIKE']) res.push('BICYCLE');
    	if ($scope.means['SHAREDBIKE']) res.push('SHAREDBIKE','SHAREDBIKE_WITHOUT_STATION');
    	return res.join(',');
    };
    
    // plan route
    $scope.plan = function() {
    	$scope.currentItinerary = null;
    	$scope.resetDrawings();
    	if (!$scope.fromMarker || !$scope.toMarker) {
    		$scope.errorMsg = 'Specifica la partenza e la destinazione!';
    		return;
    	}
    	$scope.showLoading();
		$scope.requestedFrom = $scope.fromMarker.address;
		$scope.requestedTo = $scope.toMarker.address;
    	
    	planner.plan(
    			$scope.fromMarker.getPosition(), 
    			$scope.toMarker.getPosition(),
    			convertMeans(),
    			$scope.mode,
    			$scope.mydate,
    			$scope.mytime
    			)
    	.success(function(data){
    		$scope.planned = true;
    		if (data && data.length > 0) {
    			data.sort(function(a,b) {
    				if (a.promoted != b.promoted) {
    					return b.promoted - a.promoted;
    				}
    				return 0;
    				//return a.startime != b.startime ? a.startime - b.startime : a.duration - b.duration;
    			});
    			
    			data.forEach(function(it, idx){
    				it.length = formatter.getLength(it);
    				it.means = formatter.extractItineraryMeans(it);
    				it.price = formatter.getItineraryCost(it);
    				it.index = idx;
    			});
    			
    			$scope.plans = data;
        		$scope.errorMsg = null;
        		$scope.showPlan($scope.plans[0]);
    		} else {
        		$scope.errorMsg = 'Nessun risultato trovato.';
    		}
        	$scope.hideLoading();
    	})
    	.error(function(data){
    		$scope.errorMsg = 'Errore durante la pianificazione del percorso.';
        	$scope.hideLoading();
    	});
    }
    
    $scope.toTime = function(millis) {
    	return formatter.getTimeStr(new Date(millis));
    };

    $scope.showPlan = function(plan) {
    	formatter.process(plan, $scope.fromMarker.address, $scope.toMarker.address, $scope.useCoordinates);
    	
    	$scope.currentItinerary = plan;
    	$scope.resetDrawings();
    	
    	var allElements = [];
    	for (var i = 0; i < plan.leg.length; i++) {
    		var mapElems = formatter.extractMapElements(plan.leg[i], i, $scope.map);
    		allElements = allElements.concat(mapElems);
    	}
		$scope.legElems = allElements;
    }
    
    $scope.showLoading = function() {
	    $scope.loadingInstance = $modal.open({
	      backdrop: 'static',	
	      template: '<div class="loading"><i class="glyphicon glyphicon-repeat gly-spin"></i><span>Loading...</span></div>',
	      size: 'sm'    	    
	    });
    };
    
    $scope.hideLoading = function() {
    	$scope.loadingInstance.dismiss('done');
    };
    
    $scope.popoverShown = false;
    $scope.request = function () {
    	var data = planner.getRequest(
    			$scope.fromMarker.getPosition(), 
    			$scope.toMarker.getPosition(),
    			convertMeans(),
    			$scope.mode,
    			$scope.mydate,
    			$scope.mytime);
    	$('#reqbutton').popover('destroy');
    	if (!$scope.popoverShown) {
        	$('#reqbutton').popover({html:true, content:'<pre>'+JSON.stringify(data, null, 2)+'</pre>'});
        	$('#reqbutton').popover('show');
        	$scope.popoverShown = true;
    	} else {
    		$scope.popoverShown = false;    		
    	}
    };

    $scope.report = function() {
    	var data = planner.getRequest(
    			$scope.fromMarker.getPosition(), 
    			$scope.toMarker.getPosition(),
    			convertMeans(),
    			$scope.mode,
    			$scope.mydate,
    			$scope.mytime);
    	var dataTxt = JSON.stringify(data);
    	window.open("mailto:"+MAIL+"?subject=Web Planner: segnalazione problemi&body="+dataTxt);
    };

    
    $scope.infoWindow = new google.maps.InfoWindow();
    
    $scope.toggleLayer = function(l) {
    	$scope.layers[l].show = !$scope.layers[l].show;
    	if ($scope.layers[l].show) {
    		if ($scope.layers[l].elements) {
				$scope.layers[l].elements.forEach(function(e) {
					e.setMap($scope.map);
				});
    		} else {
    			var data = $scope.layers[l].get();
    			$scope.layers[l].elements = [];
    			for (var i = 0; i < data.length; i++) {
    				var marker = new google.maps.Marker({
    		            position: new google.maps.LatLng(data[i].position[0],data[i].position[1]),
    		            icon: 'img/'+data[i].type+'.png',
    		            map: $scope.map,
    		            title: data[i].title,
    		            description: data[i].description
    		            
    		        });
    				google.maps.event.addListener(marker, 'click', function() {
    					$scope.infoWindow.setContent( '<h4>'+this.title+'</h4>'+this.description);
    					$scope.infoWindow.open($scope.map,this);
    				  });
    				$scope.layers[l].elements.push(marker);
    			} 
    		}
    		
    	} else {
			if ($scope.layers[l].elements) {
				$scope.layers[l].elements.forEach(function(e) {
					e.setMap(null);
				});
			}
    	}
    };
    
}]);
