var notification = angular.module('notification', [ 'ngMask' ]);

notification.controller('notification', function($scope, $http) {
	$scope.notify = function() {
		if ($scope.	validate()) {
			$http.post("notification/notify", {
				'title' : $scope.form.title,
				'description' : $scope.form.description,
				'from' : $scope.form.from,
				'to' : $scope.form.to
			}).success(function(data) {
				$scope.notifyMessage = data.message;
				$scope.notifyError = data.error;
			}).error(function(error) {
				$scope.notifyMessage = data.message;
				$scope.notifyError = data.error;
			});
		}
	}
	$scope.validate = function() {
		if (!$scope.form) {
			$scope.notifyMessage = "";
			$scope.notifyError = "No data!";
		} else if (!$scope.form.title || $scope.form.title.length == 0) {
			$scope.notifyMessage = "";
			$scope.notifyError = "'Title' cannot be empty!";
		} else if (Date.parse($scope.form.to) < Date.parse($scope.form.from)) {
			$scope.notifyMessage = "";
			$scope.notifyError = "'To' must be greater or equal to 'From'!";

		} else {
			return true;
		}
		return false;
	}
});