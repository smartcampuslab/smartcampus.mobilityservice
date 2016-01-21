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
                <div class="col-md-12 form-group"><label> Title: </label><input class="form-control" type="text" ng-model="form.title" name="title"/></div>
                <div class="col-md-12 form-group"><label> Description: </label><input class="form-control" type="text" ng-model="form.description" name="description"/></div>
                <div class="col-md-12 form-group"><button class="btn btn-primary" ng-click="submit"/>Send</button></div>
                <div>&nbsp;</div>
                <div class="col-md-12 form-group">
					<span ng-bind="notifyMessage"></span>
					<span ng-bind="notifyError"></span>
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