'use strict';

/**
 * Settings registration controller.
 */
angular.module('docs').controller('SettingsRegistration', function($scope, $state, Restangular, $dialog) {
  // Init
  $scope.registrations = [];
  $scope.loadCount = 0;
  $scope.total = 0;
  $scope.limit = 20;
  $scope.hasMore = false;
  $scope.isLoadingMore = false;
  
  // Filter
  $scope.filter = {
    status: 'PENDING',
    search: ''
  };
  
  /**
   * Load registrations.
   */
  $scope.loadRegistrations = function() {
    $scope.loadCount = 0;
    Restangular.one('user/registration')
      .get({
        limit: $scope.limit,
        offset: 0,
        sort_column: 1,
        asc: false,
        search: $scope.filter.search,
        status: $scope.filter.status
      })
      .then(function(data) {
        $scope.registrations = data.registrations;
        $scope.total = data.total;
        $scope.loadCount = data.registrations.length;
        $scope.hasMore = $scope.total > $scope.loadCount;
      });
  };
  
  /**
   * Load more registrations.
   */
  $scope.loadMoreRegistrations = function() {
    $scope.isLoadingMore = true;
    Restangular.one('user/registration')
      .get({
        limit: $scope.limit,
        offset: $scope.loadCount,
        sort_column: 1,
        asc: false,
        search: $scope.filter.search,
        status: $scope.filter.status
      })
      .then(function(data) {
        $scope.registrations = $scope.registrations.concat(data.registrations);
        $scope.loadCount += data.registrations.length;
        $scope.hasMore = $scope.total > $scope.loadCount;
        $scope.isLoadingMore = false;
      });
  };
  
  /**
   * Open registration details.
   */
  $scope.openRegistration = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = null;
    $scope.actionData = {};
    $('#registrationModal').modal('show');
  };
  
  /**
   * Prepare approval.
   */
  $scope.approve = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = 'approve';
    $scope.actionData = {
      storageQuota: 10, // Default 10 GB
      message: ''
    };
    $('#registrationModal').modal('show');
  };
  
  /**
   * Prepare rejection.
   */
  $scope.reject = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = 'reject';
    $scope.actionData = {
      message: ''
    };
    $('#registrationModal').modal('show');
  };
  
  /**
   * Confirm action (approve or reject).
   */
  $scope.confirmAction = function() {
    if ($scope.actionType === 'approve') {
      // Convert GB to bytes
      const storageQuotaBytes = $scope.actionData.storageQuota * 1000 * 1000 * 1000;
      
      Restangular.one('user/registration', $scope.currentRegistration.id)
        .post('approve', {
          message: $scope.actionData.message,
          storage_quota: storageQuotaBytes
        })
        .then(function() {
          $('#registrationModal').modal('hide');
          $scope.loadRegistrations();
        }, function(e) {
          $dialog.alert({
            title: 'Error',
            message: e.data.message
          });
        });
    } else if ($scope.actionType === 'reject') {
      Restangular.one('user/registration', $scope.currentRegistration.id)
        .post('reject', {
          message: $scope.actionData.message
        })
        .then(function() {
          $('#registrationModal').modal('hide');
          $scope.loadRegistrations();
        }, function(e) {
          $dialog.alert({
            title: 'Error',
            message: e.data.message
          });
        });
    }
  };
  
  // Load registrations on controller init
  $scope.loadRegistrations();
});