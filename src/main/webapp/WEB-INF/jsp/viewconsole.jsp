<!DOCTYPE html>
<html lang="en" ng-app="gameconsole">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>Gamification Console</title>

    <!-- Bootstrap core CSS -->
    <link href="../css/bootstrap.min.css" rel="stylesheet">
    <!-- Custom styles for this template -->
    <link href="../css/style.css" rel="stylesheet">
    <link href="../css/ng-scrollable.min.css" rel="stylesheet">

    <script src="../lib/angular/angular.min.js"></script>
    <script src="../lib/angular/angular-route.min.js"></script>
    <script src="../lib/ng-scrollable.min.js"></script>
    <script src="../lib/ui-bootstrap-tpls-0.12.1.min.js"></script>
    <script src="https://maps.googleapis.com/maps/api/js?libraries=geometry&v=3.exp"></script>
    <script src="../lib/sprintf.min.js"></script>

    <script src="../js/gamification/gamification.js"></script>

    <!-- Just for debugging purposes. Don't actually copy these 2 lines! -->
    <!--[if lt IE 9]><script src="lib/ie8-responsive-file-warning.js"></script><![endif]-->
    <script src="../lib/ie-emulation-modes-warning.js"></script>

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
    <style type="text/css">
      .console {
        padding: 20px;
      }
      .user-row {
        border: 1px solid #bbb;
        padding: 5px;
      }
      .user-row a {
        font-size: 24px;
      }
      .instance-row {
        border: 1px solid #ddd;
      }
      .instance-row.valid {
        background-color: lightgreen;
      }
      .instance-row.invalid {
        background-color: lightpink;
      }
      .selected {
        border: 2px solid blue;
      }
      .itinerary-row {
        padding: 5px 0;
        border-top: 1px solid #bbb;
      }
      table {
        width: 100%;
      }
      thead td {
        font-weight: bold;
      } 
      td {
        border: 1px solid #ddd;
        padding: 5px;
      }
    </style>
  </head>

  <body>
   <div class="console" ng-controller="GameCtrl">
   	
    <div class="row">
     
      <div class="col-md-3">
      <div id="left-scrollable" ng-scrollable="{scrollX:'none',scrollY:'right'}" style="width: 100%; height: 100%;">
        <div ng-repeat="user in users" class="user-row">
          <div class="row">
            <div class="col-md-6"><a ng-click="selectUser(user)">{{user}} </a></div>
            <div class="col-md-6 pull-right">({{userTotals[user].total}} tracked, <span style="{{userTotals[user].failed == 0 ? 'color: lime' : 'color:red'}}">{{userTotals[user].failed}} invalid</span>)</div>
          </div>  
          <div ng-if="selectedUser == user">
            <div ng-repeat="itinerary in userMap[user]"  class="itinerary-row">
              <h5  ng-click="selectItinerary(itinerary)">{{itinerary.tripName}} ({{itinerary.instances.length}})</h5>
              {{itinerary.startTime|date:'HH:mm'}} <span ng-if="itinerary.recurrency.daysOfWeek.length > 0">{{itinerary.recurrency.daysOfWeek}}</span>
              <div>
                <div ng-repeat="instance in itinerary.instances" ng-click="selectInstance(instance)"  class="instance-row" ng-class="{'selected':selectedInstance == instance, 'valid': instance.valid, 'invalid': !instance.valid}">
                   <div class="row">
                    <div class="col-md-6">date: {{instance.day ? instance.day : '--'}}
                    	<span ng-show="instance.validationResult.geoLocationsN <= 2" class="glyphicon glyphicon-exclamation-sign"></span>
                    	<!-- <span ng-show="!instance.validationResult.matchedLocations || !instance.validationResult.matchedActivities" class="glyphicon glyphicon-warning-sign"></span> -->
                    	<span ng-show="!instance.validationResult.matchedLocations" class="glyphicon glyphicon-move"></span>
                    	<span ng-show="!instance.validationResult.matchedActivities" class="glyphicon glyphicon-plane"></span>
                    	<span ng-show="instance.validationResult.tooFast" class="glyphicon glyphicon-road"></span>
                    </div>
                    <div class="col-md-6 pull-right">game points: {{instance.itinerary.data.customData.estimatedScore}}</div>
                   </div>                   
                </div>
              </div>
            </div>
          </div>
        </div>
        <div>
          <a ng-click="revalidate()">Re-validate</a>
        </div>
        </div>
      </div>
      <div class="col-md-9">
        <div id="map"></div>
        <div class="row" ng-if="selectedInstance != null">
          <div class="col-md-4">
            <h3 style="color:green;">Planned</h3>
            <p>{{selectedInstance.itinerary.data.startime|date:'HH:mm'}} - {{selectedInstance.itinerary.data.endtime|date:'HH:mm'}}</p>
            <hr/>
            <p ng-repeat="leg in selectedInstance.itinerary.data.leg">{{leg.transport.type}}</p>
          </div>
          <div class="col-md-4">
            <h3><span style="color:blue;">Tracked</span> (valid: <span style="{{selectedInstance.valid ? 'color: green' : 'color:red'}}">{{selectedInstance.valid}}</span>)</h3>
            <p>{{selectedInstance.geolocationEvents[0].recorded_at|date:'HH:mm'}} - {{selectedInstance.geolocationEvents[selectedInstance.geolocationEvents.length-1].recorded_at|date:'HH:mm'}}</p>
            <hr/>
             <p ng-repeat="evt in selectedInstance.legs"><b>{{evt.activity_type ? evt.activity_type : '??'}}</b> ({{evt.count}} events, {{evt.recorded_at|date:'HH:mm:ss'}}<span ng-if="evt.recorded_till != null"> -- {{evt.recorded_till|date:'HH:mm:ss'}}</span>)</p>
<!--               <p ng-repeat="evt in selectedInstance.geolocationEvents">{{evt.activity_type ? evt.activity_type : '--'}} ({{evt.recorded_at|date:'HH:mm:ss'}})</p> -->
           </div>
          <div class="col-md-4">
            <h3 style="{{selectedInstance.valid ? 'color: green' : 'color:red'}}">Validation</h3>
            <pre>{{selectedInstance.validationResult | json}}</pre>
          </div>           
        </div>
        <div class="row" ng-if="selectedInstance != null">
          <div class="col-md-12">
            <h3>Tracked events:</h3>
            <table>
              <thead>
                <td>When</td>
                <td>Accuracy</td>
                <td>Activity</td>
                <td>Activity Confidence</td>
                <td>Is moving</td>
                <td>Speed</td>
              </thead>
              <tbody>
                <tr ng-repeat="evt in selectedInstance.geolocationEvents">
                  <td>{{evt.recorded_at|date:'HH:mm:ss'}}</td>
                  <td>{{evt.accuracy}}</td>
                  <td>{{evt.activity_type}}</td>
                  <td>{{evt.activity_confidence}}</td>
                  <td>{{evt.is_moving}}</td>
                  <td>{{evt.speed}}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
   </div>
    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <script src="../lib/bootstrap.min.js"></script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <script src="../lib/ie10-viewport-bug-workaround.js"></script>
  </body>
</html>
