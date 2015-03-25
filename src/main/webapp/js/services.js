var services = angular.module('webplanner.services', []);

services.factory('geocoder', ['$http',
    function ($http) {
        var GEOCODER = 'https://os.smartcommunitylab.it/core.geocoder/';
        var getAddresses = function (userAddress) {
            var url = GEOCODER + 'spring/address?address=';
            return $http.get(url + encodeURI(userAddress));
        };

        var geocode = function(lat,lng) {
            var url = GEOCODER + 'spring/location?latlng=';
            return $http.get(url + encodeURI(lat+','+lng));
        };
        
        return {
            getAddresses: getAddresses,
            geocode: geocode
        };
    }
]);
services.factory('formatter', [function () {
    var getDateStr = function(date) {
  	  return (date.getMonth() < 9 ? '0':'')+(date.getMonth()+1) +'/'+
  	  		 (date.getDate() < 10 ? '0':'')+date.getDate() +'/' +
  	  		 date.getFullYear();
    };
    var getTimeStr = function(time) {
  	  var am = time.getHours() < 12;
  	  var hour = am ? time.getHours() : time.getHours() - 12;
  	  if (am && hour == 0) hour = 12;
  	  
  	  return (hour < 10 ? '0':'') + hour +':' +
  			 (time.getMinutes() < 10 ? '0' : '') + time.getMinutes() + (am ? 'AM' : 'PM'); 
    };
    
    var getTimeStrSimple = function(time) {
    	  var hour = time.getHours();
    	  return (hour < 10 ? '0':'') + hour +':' +
    			 (time.getMinutes() < 10 ? '0' : '') + time.getMinutes(); 
    };

    var ttMap = {
    		'WALK'		: 'ic_mt_foot',
    		'BICYCLE'	: 'ic_mt_bicycle',
    		'CAR'		: 'ic_mt_car',
    		'BUS'		: 'ic_mt_bus',
    		'TRAIN'		: 'ic_mt_train',
    		'PARK'		: 'ic_mt_parking',
    		'STREET'	: 'ic_price_parking'
    };
    
    var extractParking = function(leg) {
    	var res = [];
    	var type  = null;
    	if (leg.extra != null) {
    		if (leg.extra.costData && leg.extra.costData.fixedCost) {
    			var cost = leg.extra.costData.fixedCost > 0 ? (leg.extra.costData.fixedCost+'E/h') : 'gratis';
    			res.push(cost);
    		}
    		if (leg.extra.searchTime) {
    			res.push(leg.extra.searchTime.min+'-'+leg.extra.searchTime.max+'min');
    		}
    		type = 'STREET';
    	}
    	if (leg.to.stopId && leg.to.stopId.extra) {
    		if (leg.to.stopId.extra.costData && leg.to.stopId.extra.costData.fixedCost) {
    			var cost = leg.to.stopId.extra.costData.fixedCost > 0 ? (leg.to.stopId.extra.costData.fixedCost+'E/h') : 'gratis';
    			res.push(cost);
    		}
    		type = 'PARK';
    	}
    	if (type) {
    		return {img: 'img/'+ttMap[type]+'.png', note:res};
    	}
    };
    
    var extractItineraryMeans = function(it) {
    	var means = [];
    	var meanTypes = [];
    	for (var i = 0; i < it.leg.length; i++) {
    		var t = it.leg[i].transport.type;
    		var elem = {note:[],img:null};
    		elem.img = ttMap[t];
    		if (!elem.img) {
    			console.log('UNDEFINED: '+it.leg[i].transport.type);
    			elem.img  = ttMap['BUS'];
    		}
    		elem.img = 'img/'+elem.img+'.png';
    		
    		if (t == 'BUS' || t == 'TRAIN') {
    			elem.note = [it.leg[i].transport.routeShortName];
    		} else if (t == 'CAR') {
        		if (meanTypes.indexOf('CAR') < 0) {
        			meanTypes.push('CAR');
        			means.push(elem);
        			var parking = extractParking(it.leg[i]);
        			if (parking) elem = parking;
        		}
    		}
    		
    		if (meanTypes.indexOf(t+elem.note.join(',')) >= 0) continue;
    		meanTypes.push(t+elem.note.join(','));
        	means.push(elem);
    	}
    	return means;
    };
    
    return {
    	getTimeStrMeridian: getTimeStr,
    	getTimeStr: getTimeStrSimple,
    	getDateStr: getDateStr,
    	extractItineraryMeans: extractItineraryMeans,
    }
}]);

services.factory('planner', ['$http', 'formatter',
	  function ($http, formatter) {
	      var PLANNER = 'https://dev.smartcommunitylab.it/core.mobility';
	      
	      var plan = function(from, to, means, mode, date, time) {
	          var url = PLANNER + '/plansinglejourney';
	          var data = {
	        	from: {lat:""+from.lat(),lon: ""+from.lng()},
	        	to: {lat: ""+to.lat(),lon: ""+to.lng()},
	        	routeType: mode,
	        	resultsNumber: 3,
	        	date: formatter.getDateStr(date),
	        	departureTime: formatter.getTimeStrMeridian(time),
	        	transportTypes: means.split(',')
	          };
	          return $http.post(url,data);
          };
          
          return {
              plan: plan
          };
      }
  ]);