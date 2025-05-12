'use strict';

/**
 * User registration controller.
 */
angular.module('docs').controller('Register', function($scope, $state, Restangular) {
  $scope.user = {
    username: '',
    password: '',
    email: '',
    message: ''
  };
  
  $scope.isSuccess = false;
  $scope.isError = false;
  
  /**
   * Register form submission.
   */
  $scope.register = function() {
    $scope.isError = false;
    $scope.errorUsername = false;
    $scope.errorPassword = false;
    $scope.errorEmail = false;
    $scope.errorAlreadyExists = false;
    $scope.errorRegistrationPending = false;
    
    Restangular.one('user/registration').put($scope.user).then(function() {
      $scope.isSuccess = true;
    }, function(e) {
      $scope.isError = true;
      if (e.data.type === 'ValidationError') {
        if (e.data.message.indexOf('username') !== -1) {
          $scope.errorUsername = true;
        }
        if (e.data.message.indexOf('password') !== -1) {
          $scope.errorPassword = true;
        }
        if (e.data.message.indexOf('email') !== -1) {
          $scope.errorEmail = true;
        }
      } else if (e.data.type === 'AlreadyExistingUsername') {
        $scope.errorAlreadyExists = true;
      } else if (e.data.type === 'RegistrationPending') {
        $scope.errorRegistrationPending = true;
      }
    });
  };
  
  /**
   * Go back to login page.
   */
  $scope.backToLogin = function() {
  // 不带任何参数直接导航到login状态
    $state.go('login', {}, {location: 'replace'});
  };
});