'use strict';

/**
 * Settings controller.
 */
angular.module('docs').controller('Settings', function($scope, User, Restangular) {
  // Flag if the user is admin
  User.userInfo().then(function(data) {
    $scope.isAdmin = data.base_functions.indexOf('ADMIN') !== -1;
  });
  
  $scope.pendingRegistrationCount = 0;
  
  // 获取待处理的注册请求数量
  Restangular.one('user/registration').get({
    limit: 0,
    status: 'PENDING'
  }).then(function(data) {
    $scope.pendingRegistrationCount = data.total;
  });
});