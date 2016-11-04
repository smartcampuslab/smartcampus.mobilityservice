<!DOCTYPE html>
<html lang="en" ng-app="notification">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">
<meta http-equiv="Cache-Control"
	content="no-cache, no-store, must-revalidate" />
<meta http-equiv="Pragma" content="no-cache" />
<meta http-equiv="Expires" content="-1" />
<title>Notification</title>
<!-- Bootstrap core CSS -->
<link href="../css/bootstrap.min.css" rel="stylesheet">
<!-- Custom styles for this template -->
<link href="../css/style.css" rel="stylesheet">
<link rel='stylesheet' href='../css/textAngular.css'>
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">

<script src="../lib/angular/angular.min.js"></script>
<script src="../lib/angular/angular-route.min.js"></script>
<script src="../lib/ui-bootstrap-tpls-0.12.1.min.js"></script>
<script src='../lib/ngMask.min.js'></script>



<script src='../lib/angular/textAngular-rangy.min.js'></script>
<script src='../lib/angular/textAngular-sanitize.min.js'></script>
<script src='../lib/angular/textAngular.min.js'></script>
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="../lib/bootstrap.min.js"></script>

<script src="../js/notification.js"></script>

</head>
<body>
	<div class="container">
		<div class="row">
			<!-- <div ng-controller="notification" class="col-md-offset-4 col-md-6"> -->
			<div ng-controller="notification">
				<div class="panel panel-default ">
					<h3 style="text-align: center">Mobility Communicator Console
						for {{ appId }}</h3>

					<!-- <div class="panel-body" align="center"> -->


					<nav class="navbar navbar-default">
						<div class="container-fluid">
							<ul class="nav navbar-nav navbar-left">
								<li><button class="btn btn-default navbar-btn"
										data-toggle="modal" data-target="#notificationModal">
										<span class="glyphicon glyphicon-send"></span> Send
										Notification
									</button></li>
								<li>&nbsp;</li>
								<li><button class="btn btn-default navbar-btn"
										type="button" ng-click="resetNews()">
										<span class="glyphicon glyphicon-refresh"></span> Refresh News
									</button></li>
								<li><button class="btn btn-default navbar-btn"
										type="button" ng-click="resetNotifications()">
										<span class="glyphicon glyphicon-refresh"></span> Refresh
										Notifications
									</button></li>
							</ul>

							<ul class="nav navbar-nav navbar-right">
								<li><a href="notification/j_spring_security_logout"
									role="button"><span class="glyphicon glyphicon-log-out"></span>
										Logout</a></li>
							</ul>
						</div>
					</nav>

					<!-- <div class="panel panel-default "> -->
					<div class="panel-heading">
						<p class="panel-title">
							<strong>{{ what }}</strong>
						</p>
					</div>

					<div class="panel-body">

						<nav class="navbar navbar-default">
							<ul class="nav navbar-nav navbar-left">
								<li><button class="btn btn-default navbar-btn"
										ng-click="prevAnnouncement()"
										ng-show="prevAnnouncementVisible">
										<span class="glyphicon glyphicon-backward"></span> {{ prevText
										}}
									</button></li>
								<li><strong><p class="navbar-text"
											ng-bind="currentText"></p></strong></li>
								<li><button class="btn btn-default navbar-btn"
										ng-click="nextAnnouncement()"
										ng-show="nextAnnouncementVisible">
										<span class="glyphicon glyphicon-forward"></span> {{ nextText
										}}
									</button></li>
							</ul>
						</nav>

						<ul class="list-group">
							<li class="list-group-item" ng-repeat="msg in msgss">
								<div>
									<span> <strong>{{ msg.title != null ? msg.title
											: "&nbsp;"}}</strong> <small><span class="pull-right">{{msg.timestamp
												| date:'dd-MMM-yyyy HH:mm:ss'}}</span></small>
								</div> <!-- <div>{{ msg.description != null ? msg.description : "&nbsp;"}} -->
								<em>{{ msg.description != null ? msg.description :
									"&nbsp;"}}</em>

								<blockquote ng-show="msg.html != null">
									<div ng-bind-html="msg.html"></div>
								</blockquote>
								<div ng-show="msg.title != null">
									<span ng-show="msg.from != null && msg.to != null">From:
										{{ msg.from != null ? msg.from : "-"}} To: {{ msg.to != null ?
										msg.to : "-"}}</span> <span>&nbsp;</span>
									<div class="pull-right">
										<span class="label label-danger label-as-badge">{{
											msg.agencyId }}</span> <span class="label label-warning">{{
											msg.routeId }}</span>
									</div>
								</div> </span>
							</li>
						</ul>

						<nav class="navbar navbar-default">
							<div class="container-fluid">
								<ul class="nav navbar-nav navbar-right">
									<li><button class="btn btn-default navbar-btn"
											ng-click="prevAnnouncement()"
											ng-show="prevAnnouncementVisible">
											<span class="glyphicon glyphicon-backward"></span> {{
											prevText }}
										</button></li>
									<li><strong><p class="navbar-text"
												ng-bind="currentText"></p></strong></li>
									<li><button class="btn btn-default navbar-btn"
											ng-click="nextAnnouncement()"
											ng-show="nextAnnouncementVisible">
											<span class="glyphicon glyphicon-forward"></span> {{ nextText
											}}
										</button></li>
								</ul>
							</div>
						</nav>
					</div>
					<!-- </div> -->
				</div>

				<div id="notificationModal" class="modal fade" role="dialog">
					<div class="modal-dialog modal-lg">
						<div class="modal-content">
							<div class="modal-header">
								<h3 class="modal-title">Send notification to {{ appId }}</h3>
							</div>
							<div class="modal-body">
								<form id="notification-form" ng-submit="notify()">

									<div>
										<label> Type: </label><br /> <span> <input
											id="news-checkbox" type="checkbox" ng-model="form.news"
											name="type" value="news" ng-init="form.news=true"
											ng-checked="form.news" ><label>News</label> <input
											type="checkbox" ng-model="form.notification" name="type"
											value="notification" ng-init="form.notification=false"
											ng-checked="form.notification"><label>Notification</label>

											<div class="pull-right">
												<div class="btn-group ">
													<button type="button"
														class="btn btn-warning dropdown-toggle"
														data-toggle="dropdown" aria-haspopup="true"
														aria-expanded="false" ng-show="agencyId != null">
														{{ (routeId != null) ? routeId : 'RouteId' }}<span
															class="caret"></span>
													</button>
													<ul class="dropdown-menu routeId-dropdown">
														<li class="list-group-item"
															ng-repeat="routeId in routeIds"
															ng-click="updateRoute($event)"><a href="#">{{
																routeId }}</a></li>
													</ul>
												</div>

												<div class="btn-group " id="agencyId-dropdown">
													<button type="button" 
														class="btn btn-danger dropdown-toggle"
														data-toggle="dropdown" aria-haspopup="true"
														aria-expanded="false"" >
														{{ (agencyId != null) ? agencyId : 'AgencyId' }} <span
															class="caret"></span>
													</button>
													<ul class="dropdown-menu agencyId-dropdown">
														<li class="list-group-item"
															ng-repeat="agencyId in agencyIds"
															ng-click="updateAgency($event)")><a href="#">{{
																agencyId }}</a></li>
													</ul>
												</div>
											</div>
										</span>

									</div>
									<div>&nbsp;</div>

									<div class=" form-group">
										<label> *Title: </label><input class="form-control"
											type="text" ng-model="form.title" name="title" />
									</div>
									<div class=" form-group">
										<label> Description: </label><input class="form-control"
											type="text" ng-model="form.description" name="description" />
									</div>
									<div class=" form-group">
										<label> HTML Description: </label>
										<div text-angular ng-model="form.html"></div>
									</div>
									<div class=" form-group">
										<label> From (dd/mm/yyyy): </label><input class="form-control"
											type="text" ng-model="form.from" name="from"
											restrict="reject" mask="39/19/9999" />
									</div>
									<div class=" form-group">
										<label> To (dd/mm/yyyy): </label><input class="form-control"
											type="text" ng-model="form.to" name="to" restrict="reject"
											mask="39/19/9999" />
									</div>

									<div class=" form-group">
										<span ng-bind="notifyMessage" class="notification-ok"></span>
										<span ng-bind="notifyError" class="notification-error"></span>
										<span>&nbsp;</span>
									</div>
									<div class=" form-group">
										<button class="btn btn-primary" ng-click="submit" />
										Send
										</button>
										<button class="btn btn-primary" type="button"
											ng-click="reset()">Clear</button>
										<button class="btn btn-primary" type="button"
											data-dismiss="modal">Close</button>
									</div>
								</form>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	</div>



</body>
</html>