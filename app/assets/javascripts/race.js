var mod = angular.module("raceapp", ['ngResource', 'ui.bootstrap']);

mod.controller("RaceCarController", ["$scope", "$resource", "$modal", "$log", function($scope, $resource, $modal, $log) {
  var RaceCar = $resource('/api/racecars/:id', {
    id: '@id'
  });

  $scope.racecars = RaceCar.query();

  $scope.selectedItem = {};
  
  $scope.deleteRaceCar = function(id) {
    RaceCar.remove({id:id}, function(){
      $scope.racecars = RaceCar.query();
    });
  }
  
  $scope.editRaceCar = function(racecar) {
    $scope.selectedItem = racecar;
    $scope.open();
  }

  $scope.open = function(reset) {
    if(reset) $scope.selectedItem = {};
    
    var modalInstance = $modal.open({
      templateUrl: 'addRaceCarTemplate.html',
      controller: ModalInstanceCtrl,
      resolve: {
        item: function() {
          return $scope.selectedItem;
        }
      }
    });

    $log.info('Modal opened');

    modalInstance.result.then(function () {
      // on close
      $scope.racecars = RaceCar.query();
      $scope.selectedItem = {};
    }, function () {
      $log.info('Modal dismissed at: ' + new Date());
    });
  }

  var ModalInstanceCtrl = function($scope, $modalInstance, item) {

    $scope.drives = ["fr", "ff", "4wd", "mr"];

    $scope.item = item;

    $scope.ok = function() {
      // save
      var rc = new RaceCar($scope.item);
      rc.$save(function() {
        $scope.item = {};
        $modalInstance.close();
      });
    };

    $scope.cancel = function() {
      $log.info('Modal cancel');
      $modalInstance.dismiss('cancel');
    };
  };

}]);