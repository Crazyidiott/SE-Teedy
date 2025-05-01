'use strict';

/**
 * Settings controller.
 */
angular.module('docs').controller('Settings', function($scope, User) {
  // Flag if the user is admin
  User.userInfo().then(function(data) {
    $scope.isAdmin = data.base_functions.indexOf('ADMIN') !== -1;
  })
});

$scope.pendingRegistrationCount = 0;

// 在 $scope.init 函数中添加
Restangular.one('user/registration').get({
  limit: 0,
  status: 'PENDING'
}).then(function(data) {
  $scope.pendingRegistrationCount = data.total;
});