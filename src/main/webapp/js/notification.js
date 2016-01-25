var notification = angular.module('notification', ['ngMask']);

notification.controller('notification', function($scope, $http) {
	$scope.notify = function() {
	  $http.post("notification/notify",
			  {
		  		'title' : $scope.form.title,
		  		'description' : $scope.form.description,
		  		'from' : $scope.form.from,
		  		'to' : $scope.form.to
			  }).success(function(data){
				  $scope.notifyMessage = data.message;
				  $scope.notifyError = data.error;
			  }).error(function(error) {
				  $scope.notifyMessage = data.message;
				  $scope.notifyError = data.error;		  
			  });
	}
});