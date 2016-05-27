var plannerControllers = angular.module('plannerControllers', [])

.controller('HomeCtrl', ['$scope', '$http', '$filter', '$routeParams', '$rootScope', '$modal', '$location', 'geocoder', 'planner', 'formatter', 'parking', 'bikesharing', 'taxi',
  function($scope, $http, $filter, $routeParams, $rootScope, $modal, $location, geocoder, planner, formatter, parking, bikesharing, taxi) {

	// current user position, defaults to Trento
	$scope.myposition = $rootScope.CENTER;
	
	$scope.mytime = new Date();
	$scope.mydate = new Date();
    $scope.fromMarker = null;
    $scope.toMarker = null;
    $scope.mode = 'fastest';
    $scope.policyform = { 'name' : 'Dummy', 'description' : 'Nessuna'};
    $scope.currentPolicy = { name: 'Dummy', description: 'Nessuna', editable: false, draft : false};
    $scope.wheelchair = false;
    
    $scope.useCoordinates = false;
    
    $scope.means = {'TRANSIT':true};
    $scope.layers = {'PARKING':{show: false, elements: null, get: parking.getAll},'BIKESHARING':{show: false, elements: null, get: bikesharing.getAll},'TAXI':{show: false, elements: null, get: taxi.getAll}};
	$scope.currentItinerary = null;
	$scope.legElems = [];
	$scope.planned = false;
	$scope.loadingInstance = null;
	
	
//	CAR(0,11),BICYCLE(16,0),TRANSIT(0,2),SHAREDBIKE(16,0),SHAREDBIKE_WITHOUT_STATION(16,0),GONDOLA(0,2),
//	CARWITHPARKING(0,11),SHAREDCAR(0,4),SHAREDCAR_WITHOUT_STATION(0,4),BUS(0,2),TRAIN(0,2),WALK(12,0),SHUTTLE(0,2),
//	PARK_AND_RIDE(0,4);	
	
	$scope.ttypes = [null, "BICYCLE", "SHAREDBIKE", "BUS", "TRAIN", "TRANSIT", "WALK", "CAR", "CARWITHPARKING", "PARK_AND_RIDE"];
	$scope.rtypes = [null, "fastest", "healthy", "leastWalking", "leastChanges", "greenest", "safest"];
	$scope.stypes = [null, "fastest", "healthy", "leastWalking", "leastChanges", "greenest", "safest", "fastestAndCheapest"];
	$scope.smartplannerParameters = ["maxWalkDistance", "maxTotalWalkDistance", "extraTransport", "maxChanges"];
	$scope.parametrics = [];
	$scope.parametric = {};
	
	$scope.compileform = { "create" : [], "modify" : [], "evaluate" : [], "filter" : { "keep" : 2, "sortType" : "fastest" , "formulas" : []}, "groups" : []};
	
	$scope.init = function($http) {
		$http.get($rootScope.controllerBase + ($rootScope.publishedOnly?'?draft=false':'')).success(function(data) {	
			$scope.policyIds = data;
		}).error(function(data){
			$scope.policyIds = data;
		});
//		$http.get("notification/announcements/appId").success(function(data) {	
//			$scope.appId = data;
//		});		
	}
	
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
            icon: $rootScope.imgBase + 'img/'+icon+'.png',
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
    
	$scope.init($http);
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
//    if(navigator.geolocation) {
    if(false) {
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
    			$scope.mytime,
    			$scope.currentPolicy.name,
    			$scope.wheelchair
    			)
    	.success(function(data){
    		$scope.planned = true;
    		if (data && data.length > 0) {
//    			data.sort(function(a,b) {
//    				if (a.promoted != b.promoted) {
//    					return b.promoted - a.promoted;
//    				}
//    				if ($scope.mode == 'fastest') {
//    					return (a.endtime - a.startime) - (b.endtime - b.startime);
//    				} else if ($scope.mode == 'leastChanges') {
//    					return (a.leg.length - b.leg.length); 
//    				} else if ($scope.mode == 'leastWalking') {
//    					var al = 0;
//    					var bl = 0;
//    					for (var i = 0; i < a.leg.length; i++) {
//    						al += (a.leg[i].transport.type == 'WALK') ? a.leg[i].length : 0;
//    					}
//    					for (var i = 0; i < b.leg.length; i++) {
//    						bl += (b.leg[i].transport.type == 'WALK') ? b.leg[i].length : 0;
//    					}    			
//    					return al - bl;
//    				} else {
//    					return 0;
//    				}
//    			});
    			
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
        		$scope.plans = null;
    		}
        	$scope.hideLoading();
    	})
    	.error(function(data, status, headers, config) {
    		$scope.errorMsg = 'Errore durante la pianificazione del percorso: ' + headers('error_msg');
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
    			$scope.mytime,
    			$scope.currentPolicy.name,
    			$scope.wheelchair);
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
    			$scope.mytime,
    			$scope.currentPolicy.name,
    			$scope.wheelchair);
    	var dataTxt = JSON.stringify(data);
    	window.open("mailto:"+MAIL+"?subject=Web Planner: segnalazione problemi&body="+dataTxt);
    };
    
    $scope.recenter = function(lat, lon) {
    	var newCenter = new google.maps.LatLng(lat, lon);
    	$scope.map.setCenter(newCenter);
    	$scope.map.panTo(newCenter);
    	$scope.map.setZoom(15);
    }
    
    $scope.centerItinerary = function() {
        var bounds = new google.maps.LatLngBounds();
        for (var i = 0; i < $scope.currentItinerary.leg.length; i++) {
	        var points = decodePolyline($scope.currentItinerary.leg[i].legGeometery.points);
	        for (var j = 0; j < points.length; j++) {
	        	bounds.extend(points[j]);
	        }
        }
    	$scope.map.fitBounds(bounds);
    }    
    
    $scope.centerUser = function() {
    	if (navigator.geolocation) {
    	    navigator.geolocation.getCurrentPosition(function(position) {
    	      var pos = {lat: position.coords.latitude,lng: position.coords.longitude};
    	      $scope.map.setCenter(pos);
    	      $scope.map.panTo(pos);
    	      $scope.map.setZoom(15);    	      
    	    });
    	}
    }
    
    function decodePolyline(encoded) {
        if (!encoded) {
            return [];
        }
        var poly = [];
        var index = 0, len = encoded.length;
        var lat = 0, lng = 0;

        while (index < len) {
            var b, shift = 0, result = 0;

            do {
                b = encoded.charCodeAt(index++) - 63;
                result = result | ((b & 0x1f) << shift);
                shift += 5;
            } while (b >= 0x20);

            var dlat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;

            do {
                b = encoded.charCodeAt(index++) - 63;
                result = result | ((b & 0x1f) << shift);
                shift += 5;
            } while (b >= 0x20);

            var dlng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            var p = new google.maps.LatLng({lat: lat / 1e5, lng: lng / 1e5});
            poly.push(p);
        }
        return poly;
    }    

    
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
    		            icon: $rootScope.imgBase + 'img/'+data[i].type+'.png',
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
    
    
    ////////////////
    
//    $scope.resetPolicy = function() {
//		$scope.message = "";
//		$scope.error = "";  	
//		$scope.policyform = { elements : []};
//		$scope.parametrics = {};
//		$scope.updatePolicy = false;
//		$scope.currentPolicy = { policyType : 'compiled', editable: true, enabled : true};
////		$('#compiledmodal').modal('show');
//	}	    
    
    
    
//    $scope.resetPolicy = function($type) {
//		$scope.message = "";
//		$scope.error = "";  	
//		$scope.policyform = { elements : []};
//		$scope.parametrics = {};
//		$scope.updatePolicy = false;
//		$scope.currentPolicy = { policyType : $type, editable: true, enabled : true};
////		$('#multimodal').modal('show');
//	}			
	
	
   
    

    
//    $scope.readPolicy = function() {
//		$scope.message = "";
//		$scope.error = "";  
//    	$http.get($rootScope.controllerBase + "/" + $scope.currentPolicy.policyType + "/" + $scope.currentPolicy.name, {
//			headers : {
//				'Content-Type' : "application/json",
//				'Accept' : "application/json"
//			}
//		}).success(function(data) {
//			$scope.policyform = data;
//			$scope.updatePolicy = true;
//			console.log(JSON.stringify($scope.policyform, null, 2));
//		}).error(function(data) {
//		});
//    }

    /*
    $scope.saveScriptedPolicy = function() {
	if ($scope.policyform) {
		$http({
			'method' : ($scope.updatePolicy ? 'PUT' : 'POST'),
			'url' : $rootScope.controllerBase + "/scripted/",
			'data' : {
				'name' : $scope.policyform.name,
				'description' : $scope.policyform.description,
				'generatePlanRequests' : $scope.policyform.generatePlanRequests,
				'evaluatePlanResults' : $scope.policyform.evaluatePlanResults,
				'extractItinerariesFromPlanResults' : $scope.policyform.extractItinerariesFromPlanResults,
				'filterAndSortItineraries' : $scope.policyform.filterAndSortItineraries,
				'draft' : $scope.policyform.draft
			},
			'headers' : {
				'Content-Type' : "application/json",
				'Accept' : "application/json"
			}
		}).success(function(data, status, headers, config) {
			$scope.message = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("msg");
			$scope.error = "";
			$http.get($rootScope.controllerBase).success(function(data) {
				$scope.policyIds = data;
			});
		}).error(function(data, status, headers, config) {
			$scope.message = "";
			$scope.error = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("error_msg");
		});
	}
}
    
	$scope.addNewParametric = function($type) {
//		console.log(JSON.stringify($scope.policyform, null, 2));
		$scope.policyform.elements.push({"type" : $type});
	}

	$scope.copyParametric = function($parametric) {
		var index = $scope.policyform.elements.indexOf($parametric);
		$scope.policyform.elements.splice(index + 1, 0, JSON.parse(JSON.stringify($parametric)));
//		$scope.policyform.elements.push(JSON.parse(JSON.stringify($parametric)));
		$scope.$apply();
	}	
	
	
	$scope.removeParametric = function($parametric) {
		$scope.policyform.elements = $scope.policyform.elements.filter(function($elem) {
			return $elem !== $parametric;
		});
	}
    
	///////////////
	
	$scope.saveParametricPolicy = function() {
		console.log(JSON.stringify($scope.policyform, null, 2));
			$http({
//				'method' : ($scope.updatePolicy ? 'PUT' : 'POST'),
				'method' : 'POST',
				'url' : $rootScope.controllerBase + "/parametric",
				'data' : {
					'name' : $scope.policyform.name,
					'description' : $scope.policyform.description,
					'elements' : $scope.policyform.elements.filter(function($elem) {
						return $elem.type !== "group"}),
					'groups' : $scope.policyform.elements.filter(function($elem) {
							return $elem.type == "group"}),						
					'draft' : $scope.policyform.draft
				},
				'headers' : {
					'Content-Type' : "application/json",
					'Accept' : "application/json"
				}
			}).success(function(data, status, headers, config) {
				$scope.message = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("msg");
				$scope.error = "";
				$http.get($rootScope.controllerBase).success(function(data) {
					$scope.policyIds = data;
				});
			}).error(function(data, status, headers, config) {
				$scope.message = "";
				$scope.error = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("error_msg");
			});
	}	
	*/
	
	
	///////////////
	
    $scope.resetPolicy = function() {
		$scope.message = "";
		$scope.error = "";  	
		$scope.compileform = {"name" : null, "description" : null, "create" : [], "modify" : [], "evaluate" : [], "filter" : { "keep" : 2, "sortType" : "fastest" , "formulas" : []}, "groups" : []};
		$scope.updatePolicy = false;
	}	  	
    
	$scope.readPolicies = function(draft) {
		$http.get($rootScope.controllerBase + "?draft=" + draft).success(function(data) {	
			$scope.policyIds = data;
		}); 
	}        
	
    $scope.readPolicy = function() {
		$scope.message = "";
		$scope.error = "";  
    	$http.get($rootScope.controllerBase + "/compiled/" + $scope.currentPolicy.name, {
			headers : {
				'Content-Type' : "application/json",
				'Accept' : "application/json"
			}
		}).success(function(data) {
			$scope.compileform = data;
			$scope.updatePolicy = true;
			console.log(JSON.stringify($scope.compileform, null, 2));
		}).error(function(data) {
		});
    }	
	
	$scope.setPolicy = function($policy) {
		$scope.currentPolicy = $policy;
		if (!$policy.editable) {
			$scope.compileform = $policy;
		}
		$('#policyIds-dropdown').attr('data-original-title', $policy.name);
	} 	
	
	$scope.addCreate = function() {
		$scope.compileform.create.push({"action" : { "promoted" : true}});
	}
	
	$scope.addModify = function() {
		$scope.compileform.modify.push({"action" : { "promoted" : true}});
	}	
	
	$scope.addEvaluate = function() {
		$scope.compileform.evaluate.push({"action" : { "promoted" : true}});
	}		
	
	$scope.addFilterFormula = function() {
		$scope.compileform.filter.formulas.push("promoted && ");
		$scope.compileform.filter.enabled.push(true);
	}		
	
	$scope.addGroup = function() {
		$scope.compileform.groups.push({});
	}		
	
	
	$scope.removeCreate = function($create) {
		$scope.compileform.create = $scope.compileform.create.filter(function($elem) {
			return $elem !== $create;
		});
	}
	
	$scope.removeModify = function($modify) {
		$scope.compileform.modify = $scope.compileform.modify.filter(function($elem) {
			return $elem !== $modify;
		});
	}	
	
	$scope.removeEvaluate = function($evaluate) {
		$scope.compileform.evaluate = $scope.compileform.evaluate.filter(function($elem) {
			return $elem !== $evaluate;
		});
	}	
	
	$scope.removeFilterFormula = function($formula) {
		$scope.compileform.filter.formulas = $scope.compileform.filter.formulas.filter(function($elem) {
			return $elem !== $formula;
		});
	}		
	
//	$scope.toggleComment = function($index) {
//		if ($scope.compileform.filter.formulas[$index].startsWith("//")) {
//			$scope.compileform.filter.formulas[$index] = $scope.compileform.filter.formulas[$index].replace("//", "").trim();
//		} else {
//			$scope.compileform.filter.formulas[$index] = "// " + $scope.compileform.filter.formulas[$index];
//		}
//	}
	
	$scope.toggleFormula = function($index) {
		console.log($scope.compileform.filter.enabled[$index]);
		$scope.compileform.filter.enabled[$index] = !$scope.compileform.filter.enabled[$index];
		console.log($scope.compileform.filter.enabled[$index]);
	}	
	
	$scope.removeGroup = function($group) {
		$scope.compileform.group = $scope.compileform.group.filter(function($elem) {
			return $elem !== $group;
		});
	}	
	
	$scope.getGroupNames = function() {
		res = [null];
		for (i = 0; i < $scope.compileform.groups.length; i++) {
			res.push($scope.compileform.groups[i].name);
		}
		return res;
	}	
	
	
	$scope.saveCompiledPolicy = function() {
		console.log(JSON.stringify($scope.compileform, null, 2));
			$http({
				'method' : 'POST',
				'url' : $rootScope.controllerBase + "/compiled",
				'data' : $scope.compileform,
				'headers' : {
					'Content-Type' : "application/json",
					'Accept' : "application/json"
				}
			}).success(function(data, status, headers, config) {
				$scope.message = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("msg");
				$scope.compileform = data;
				$scope.error = "";
				$http.get($rootScope.controllerBase).success(function(data) {
					$scope.policyIds = data;
				});
				$scope.$apply();
			}).error(function(data, status, headers, config) {
				$scope.message = "";
				$scope.error = $filter('date')(new Date(), "hh:mm:ss") + ": " + headers("error_msg");
			});
	}		
	
	$scope.deletePolicy = function() {
		bootbox.confirm('Sei sicuro di voler eliminare la politica <strong>' + $scope.currentPolicy.name + ' (' + $scope.currentPolicy.description + ')</strong>?', function(result) {
			if (result) {
				$scope.message = "";
				$scope.error = "";
				$http({
					'method' : 'DELETE',
					'url' : $rootScope.controllerBase + "/compiled/" + $scope.currentPolicy.name,
				}).success(function(data) {
					$scope.policyform = {
						'name' : 'Dummy',
						'description' : 'Nessuna'
					};
					$scope.currentPolicy = { name: 'Dummy', description: 'Nessuna', editable : false, draft : false};
					$http.get($rootScope.controllerBase).success(function(data) {
						$scope.policyIds = data;
					});
				}).error(function(data) {
				});
			}
		});
	}	
	
	
	//////////////
    
    
    $(document).ready(function(){
    	$('#policyIds-dropdown').tooltip({placement: 'left', title: $scope.currentPolicy.name, html: true});
    	
    	var hash = window.location.hash;
        var link = $('a');
        $('.nav-tabs > li > a').click(function (e) {
        	console.log("???");
        	alert( "Handler for .click() called." );        	
          e.preventDefault();
          hash = link.attr("href");
          window.location = hash;
        });    	
    	
    	
/*    	$('.nav-tabs li a').click(function (e) {
			alert( "Handler for .click() called." ); 
    	    e.preventDefault();
    	    $(this).tab('show');
    	    $('.tab-content > .tab-pane.active').jScrollPane();
    	}); */   	
    	
    	
/*    	if (location.hash !== '') {
    	    $('.nav-tabs a[href="' + location.hash.replace('tab_','') + '"]').tab('show');
    	} else {
    	    $('.nav-tabs a:first').tab('show');
    	}

    	$('.nav-tabs a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
    	      window.location.hash = 'tab_'+  e.target.hash.substr(1) ; 
    	      return false;
    	});   */ 	
    	
/*    	$('.nav-tabs a').click(function (e) {
    	      e.preventDefault();
    	      $(this).tab('show');
    	    });   */ 	
/*        $('#a').click(function (a) {
        	alert( "Handler for .click() called." );
      	  
      	  $(this).tab('show');
      	});*/
    }); 
   
    
}]);




