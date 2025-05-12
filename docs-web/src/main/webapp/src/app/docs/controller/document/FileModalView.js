'use strict';

/**
 * File modal view controller.
 */
angular.module('docs').controller('FileModalView', function ($uibModalInstance, $scope, $state, $stateParams, $sce, Restangular, $transitions, $timeout, $uibModal) {
  // 添加翻译功能相关的变量
  $scope.canTranslate = false;
  $scope.languages = [];

  var setFile = function (files) {
    // Search current file
    _.each(files, function (value) {
      if (value.id === $stateParams.fileId) {
        $scope.file = value;
        $scope.trustedFileUrl = $sce.trustAsResourceUrl('../api/file/' + $stateParams.fileId + '/data');
        
        // 检查文件是否可翻译
        checkFileTranslatability();
      }
    });
  };

  // 检查文件是否可翻译
  var checkFileTranslatability = function() {
    if (!$scope.file) return;
    
    // 支持的文件类型
    var supportedMimeTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      'application/vnd.ms-powerpoint',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel',
      'image/jpeg',
      'image/png',
      'image/bmp'
    ];
    
    // 检查API配置和文件类型是否支持翻译
    Restangular.one('app/config')
      .get({
        key: 'YOUDAO_APP_KEY'
      })
      .then(function(data) {
        var hasApiKey = data.value && data.value.trim() !== '';
        $scope.canTranslate = hasApiKey && supportedMimeTypes.indexOf($scope.file.mimetype) !== -1;
      });
  };

  // Load files
  Restangular.one('file/list').get({ id: $stateParams.id }).then(function (data) {
    $scope.files = data.files;
    setFile(data.files);

    // File not found, maybe it's a version
    if (!$scope.file) {
      Restangular.one('file/' + $stateParams.fileId + '/versions').get().then(function (data) {
        setFile(data.files);
      });
    }
  });

  /**
   * Return the next file.
   */
  $scope.nextFile = function () {
    var next = undefined;
    _.each($scope.files, function (value, key) {
      if (value.id === $stateParams.fileId) {
        next = $scope.files[key + 1];
      }
    });
    return next;
  };

  /**
   * Return the previous file.
   */
  $scope.previousFile = function () {
    var previous = undefined;
    _.each($scope.files, function (value, key) {
      if (value.id === $stateParams.fileId) {
        previous = $scope.files[key - 1];
      }
    });
    return previous;
  };

  /**
   * Navigate to the next file.
   */
  $scope.goNextFile = function () {
    var next = $scope.nextFile();
    if (next) {
      $state.go('^.file', { id: $stateParams.id, fileId: next.id });
    }
  };

  /**
   * Navigate to the previous file.
   */
  $scope.goPreviousFile = function () {
    var previous = $scope.previousFile();
    if (previous) {
      $state.go('^.file', { id: $stateParams.id, fileId: previous.id });
    }
  };

  /**
   * Open the file in a new window.
   */
  $scope.openFile = function () {
    window.open('../api/file/' + $stateParams.fileId + '/data');
  };

  /**
   * Open the file content a new window.
   */
  $scope.openFileContent = function () {
    window.open('../api/file/' + $stateParams.fileId + '/data?size=content');
  };

  /**
   * Print the file.
   */
  $scope.printFile = function () {
    var popup = window.open('../api/file/' + $stateParams.fileId + '/data', '_blank');
    popup.onload = function () {
      popup.print();
      popup.close();
    }
  };

  /**
   * Close the file preview.
   */
  $scope.closeFile = function () {
    $uibModalInstance.dismiss();
  };

  // 打开翻译对话框
  $scope.translateFile = function() {
      console.log('translateFile function called');
      
      // 确保文件信息存在
      if (!$scope.file) {
          console.error('File information is missing');
          return;
      }
      
      // 先加载支持的语言
      Restangular.one('file/translate/languages').get()
          .then(function(data) {
              console.log('Languages loaded:', data.languages);
              
              // 创建并打开模态框
              var modalInstance = $uibModal.open({
                  animation: true,
                  templateUrl: 'translateModal.html',
                  controller: 'TranslateModalController',
                  backdrop: 'static',
                  resolve: {
                      translate: function() {
                          return {
                              sourceLanguage: '',
                              targetLanguage: '',
                              statusCode: 0,
                              statusText: '',
                              flowNumber: '',
                              fileType: getFileTypeFromMimetype($scope.file.mimetype),
                              error: null
                          };
                      },
                      languages: function() {
                          return data.languages;
                      },
                      apiNotConfigured: function() {
                          return false; // 由于我们已经检查了canTranslate，这里可以设为false
                      },
                      file: function() {
                          return $scope.file; // 关键是这里，确保正确传递file对象
                      }
                  }
              });
              
              console.log('Modal opened');
          })
          .catch(function(error) {
              console.error('Error loading languages:', error);
          });
  };
  
  // 根据MIME类型获取文件类型
  function getFileTypeFromMimetype(mimeType) {
    if (!mimeType) {
      return null;
    }
    
    if (mimeType.includes('word') || mimeType.includes('docx') || mimeType.includes('doc')) {
      return 'docx';
    } else if (mimeType.includes('pdf')) {
      return 'pdf';
    } else if (mimeType.includes('powerpoint') || mimeType.includes('ppt')) {
      return 'pptx';
    } else if (mimeType.includes('excel') || mimeType.includes('xlsx')) {
      return 'xlsx';
    } else if (mimeType.includes('jpg') || mimeType.includes('jpeg')) {
      return 'jpg';
    } else if (mimeType.includes('png')) {
      return 'png';
    } else if (mimeType.includes('bmp')) {
      return 'bmp';
    }
    
    return null;
  }

  // Close the modal when the user exits this state
  var off = $transitions.onStart({}, function(transition) {
    if (!$uibModalInstance.closed) {
      if (transition.to().name === $state.current.name) {
        $uibModalInstance.close();
      } else {
        $uibModalInstance.dismiss();
      }
    }
    off();
  });

  /**
   * Return true if we can display the preview image.
   */
  $scope.canDisplayPreview = function () {
    return $scope.file && $scope.file.mimetype !== 'application/pdf';
  };
});

/**
 * Translation modal controller
 */
angular.module('docs').controller('TranslateModalController', function($scope, $uibModalInstance, $timeout, Restangular, translate, languages, apiNotConfigured, file) {
    console.log('TranslateModalController initialized with file:', file);
    
    $scope.translate = translate;
    $scope.languages = languages;
    $scope.apiNotConfigured = apiNotConfigured;
    $scope.file = file; // 确保这里正确接收到file对象
    $scope.translationInProgress = false;
    
    $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
    };
    
    $scope.startTranslation = function() {
        if (!$scope.translate.sourceLanguage || !$scope.translate.targetLanguage) {
            return;
        }
        
        console.log('Starting translation for file ID:', file.id);
        
        $scope.translationInProgress = true;
        $scope.translate.error = null;
        
        // 调用API开始翻译
        Restangular.one('file/translate/start').post('', {
            id: file.id, // 使用传入的file对象
            source_language: $scope.translate.sourceLanguage,
            target_language: $scope.translate.targetLanguage
        }).then(function(data) {
            console.log('Translation started, flow number:', data.flow_number);
            $scope.translate.flowNumber = data.flow_number;
            // 开始轮询翻译状态
            checkTranslationStatus();
        }, function(response) {
            console.error('Translation start error:', response);
            $scope.translationInProgress = false;
            $scope.translate.error = response.data.message || 'Error starting translation';
        });
    };
    
    // 检查翻译状态
    function checkTranslationStatus() {
        if (!$scope.translate.flowNumber) {
            return;
        }
        
        Restangular.one('file/translate/status')
            .get({flow_number: $scope.translate.flowNumber})
            .then(function(data) {
                console.log('Translation status:', data);
                
                if (data.status === 'ok') {
                    $scope.translate.statusCode = data.status_code;
                    $scope.translate.statusText = data.status_text;
                    
                    // 如果翻译尚未完成，继续轮询
                    if (data.status_code !== 4 && data.status_code > 0) {
                        $timeout(checkTranslationStatus, 2000);
                    } else if (data.status_code === 4) {
                        // 翻译完成
                        $scope.translationInProgress = false;
                    } else if (data.status_code < 0) {
                        // 翻译出错
                        $scope.translationInProgress = false;
                        $scope.translate.error = data.status_text;
                    }
                } else {
                    $scope.translationInProgress = false;
                    $scope.translate.error = data.error_code ? 'Error code: ' + data.error_code : 'Unknown error';
                }
            }, function(response) {
                console.error('Translation status check error:', response);
                $scope.translationInProgress = false;
                $scope.translate.error = response.data.message || 'Error checking translation status';
            });
    }
    
    $scope.downloadTranslation = function() {
        var url = '../api/file/translate/download?flow_number=' + 
            $scope.translate.flowNumber + 
            '&file_type=' + $scope.translate.fileType + 
            '&file_id=' + file.id + 
            '&target_language=' + $scope.translate.targetLanguage;
            
        console.log('Downloading translation from URL:', url);
        window.location.href = url;
        
        $uibModalInstance.close($scope.translate);
    };
});