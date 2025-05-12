'use strict';

/**
 * Settings registration controller.
 */
angular.module('docs').controller('SettingsRegistration', function($scope, $state, Restangular, $dialog) {
  // 初始化
  $scope.registrations = [];
  $scope.loadCount = 0;
  $scope.total = 0;
  $scope.limit = 20;
  $scope.hasMore = false;
  $scope.isLoadingMore = false;
  $scope.showModal = false; // 添加这个变量
  
  // 过滤器
  $scope.filter = {
    status: 'PENDING',
    search: ''
  };
  
  /**
   * 加载注册请求列表.
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
   * 加载更多注册请求.
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
   * 打开注册详情.
   */
  $scope.openRegistration = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = null;
    $scope.actionData = {};
    $scope.showModal = true; // 使用Angular变量显示模态框
  };
  
  /**
   * 关闭模态框.
   */
  $scope.closeModal = function() {
    $scope.showModal = false;
  };
  
  /**
   * 准备批准.
   */
  $scope.approve = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = 'approve';
    $scope.actionData = {
      storageQuota: 10, // 默认10 GB
      message: ''
    };
    $scope.showModal = true; // 使用Angular变量显示模态框
  };
  
  /**
   * 准备拒绝.
   */
  $scope.reject = function(registration) {
    $scope.currentRegistration = registration;
    $scope.actionType = 'reject';
    $scope.actionData = {
      message: ''
    };
    $scope.showModal = true; // 使用Angular变量显示模态框
  };
  
  /**
   * 确认操作（批准或拒绝）.
   */
  $scope.confirmAction = function() {
    if ($scope.actionType === 'approve') {
      // 转换GB为字节
      const storageQuotaBytes = $scope.actionData.storageQuota * 1000 * 1000 * 1000;
      
      Restangular.one('user/registration', $scope.currentRegistration.id)
        .post('approve', {
          message: $scope.actionData.message,
          storage_quota: storageQuotaBytes
        })
        .then(function() {
          $scope.closeModal(); // 关闭模态框
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
          $scope.closeModal(); // 关闭模态框
          $scope.loadRegistrations();
        }, function(e) {
          $dialog.alert({
            title: 'Error',
            message: e.data.message
          });
        });
    }
  };
  
  // 控制器初始化时加载注册请求列表
  $scope.loadRegistrations();
});