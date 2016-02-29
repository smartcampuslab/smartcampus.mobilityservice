<!DOCTYPE html>
<html lang="en" ng-app="notification">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<meta name="description" content="">
		<meta name="author" content="">
		<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />
		<meta http-equiv="Pragma" content="no-cache" />
		<meta http-equiv="Expires" content="-1" />	      
	    <title>Notification</title>
	    <!-- Bootstrap core CSS -->
	    <link href="css/bootstrap.min.css" rel="stylesheet">
	    <!-- Custom styles for this template -->
	    <link href="css/style.css" rel="stylesheet">
	
	    <script src="lib/angular/angular.min.js"></script>
	    <script src="lib/angular/angular-route.min.js"></script>
	    <script src="lib/ui-bootstrap-tpls-0.12.1.min.js"></script>
	    <script src='lib/ngMask.min.js'></script>
	      
		<link rel='stylesheet' href='css/textAngular.css'>
		<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
		
		<script src='lib/angular/textAngular-rangy.min.js'></script>
		<script src='lib/angular/textAngular-sanitize.min.js'></script>
		<script src='lib/angular/textAngular.min.js'></script>	      

	    <script src="js/notification.js"></script>
	      
    </head>
    <body>
      <div class="container">
        <div class="row">
          <div class="col-md-offset-4 col-md-6">
            <div class="panel panel-default">
            <h3 style="text-align:center">Mobility Communicator Console Login</h3>
            <form ng-controller="notification" ng-submit="notify()">
                <div>&nbsp;</div>
                <div class=" form-group"><label> *Title: </label><input class="form-control" type="text" ng-model="form.title" name="title"/></div>
                <div class=" form-group"><label> Description: </label><input class="form-control" type="text" ng-model="form.description" name="description"/></div>
                <!-- <div class=" form-group"><label> HTML Description: </label><input class="form-control" type="text" ng-model="form.html" name="html"/></div> -->
                <div class=" form-group"><label> HTML Description: </label><div text-angular ng-model="form.html"></div></div>
                <div class=" form-group"><label> From (dd/mm/yyyy): </label><input class="form-control" type="text" ng-model="form.from" name="from" restrict="reject" mask="39/19/9999" /></div>
                <div class=" form-group"><label> To (dd/mm/yyyy): </label><input class="form-control" type="text" ng-model="form.to" name="to" restrict="reject" mask="39/19/9999" /></div>
                
                <div><label> Type: </label><br/>
               		<input type="checkbox" ng-model="form.news" name="type" value="news" ng-init="form.news=true" ng-checked="true"><label>News</label>
					<input type="checkbox" ng-model="form.notification" name="type" value="notification" ng-init="form.notification=false"><label>Notification</label><br/>
				</div>
				
				<div class=" form-group"><label> AgencyId: </label><input class="form-control" type="text" ng-model="form.agencyId" name="agencyId"/></div>
				<div class=" form-group"><label> Route Id: </label><input class="form-control" type="text" ng-model="form.routeId" name="routeId"/></div>
				
                <div class=" form-group"><button class="btn btn-primary" ng-click="submit"/>Send</button></div>
                <div>&nbsp;</div>
                <div class=" form-group">
					<span ng-bind="notifyMessage" class="notification-ok"></span>
					<span ng-bind="notifyError" class="notification-error"></span>
				</div>                
            </form>
            <a href="j_spring_security_logout">Logout</a>
            </div>
          </div>
        </div>
      </div>  
      <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
      <script src="lib/bootstrap.min.js"></script>
    </body>
</html>