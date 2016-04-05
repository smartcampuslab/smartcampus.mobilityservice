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

    <script src="../lib/angular/angular.min.js"></script>
    <script src="../lib/angular/angular-route.min.js"></script>
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
  </head>

  <body>
   <div class="container" ng-controller="GameCtrl">
    <div class="row">
      <div class="col-md-2">
        <div ng-repeat="user in users">
          <h3><a ng-click="selectUser(user)">{{user}}</a></h3>
          <div ng-if="selectedUser == user">
            <div ng-repeat="itinerary in userMap[user]" ng-click="selectItinerary(itinerary)">
              <h5>{{itinerary.tripName}} ({{itinerary.instances.length}})</h5>
              <p>{{itinerary.startTime|date:'dd/MM/yyyy HH:mm'}}</p>
              <p ng-if="itinerary.recurrency.daysOfWeek.length > 0">{{itinerary.recurrency.daysOfWeek}}</p>
              <div ng-if="itinerary.instances.length > 1">
                <div ng-repeat="instance in itinerary.instances" ng-click="selectInstance(instance)">
                   {{instance.day}}                   
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-10">
        <div id="map"></div>
        <div class="row" ng-if="selectedInstance != null">
          <div class="col-md-6">
            <h3 style="color:#00FF00;">Planned</h3>
            <p>{{selectedInstance.itinerary.data.startime|date:'HH:mm'}} - {{selectedInstance.itinerary.data.endtime|date:'HH:mm'}}</p>
            <hr/>
            <p ng-repeat="leg in selectedInstance.itinerary.data.leg">{{leg.transport.type}}</p>
          </div>
          <div class="col-md-6">
            <h3 style="color:#FF0000;">Tracked (valid: {{selectedInstance.valid}})</h3>
            <p>{{selectedInstance.geolocationEvents[0].recorded_at|date:'HH:mm'}} - {{selectedInstance.geolocationEvents[selectedInstance.geolocationEvents.length-1].recorded_at|date:'HH:mm'}}</p>
            <hr/>
             <p ng-repeat="evt in selectedInstance.legs"><b>{{evt.activity_type ? evt.activity_type : '??'}}</b> ({{evt.count}} events, {{evt.recorded_at|date:'HH:mm:ss'}}<span ng-if="evt.recorded_till != null"> -- {{evt.recorded_till|date:'HH:mm:ss'}}</span>)</p>
<!--               <p ng-repeat="evt in selectedInstance.geolocationEvents">{{evt.activity_type ? evt.activity_type : '--'}} ({{evt.recorded_at|date:'HH:mm:ss'}})</p> -->
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
