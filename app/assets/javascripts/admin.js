var mod = angular.module("adminapp", ['ngResource','ngMap']);

mod.controller("UserController", ["$scope", "$resource",function($scope, $resource){
  var User = $resource('/api/users/:id', {id:'@id'});
  
  $scope.users = User.query();

  $scope.roles = ["admin", "normal", "race"]
  
  $scope.booleans = [true, false]

  function initCreateForm() {
    $scope.cf = {
            role: "normal",
            disabled: false
    }
  }
  
  initCreateForm();
  
  $scope.createUser = function(createForm){
    console.log(createForm);
    var newUser = new User(createForm);
    newUser.$save(function(){
      initCreateForm();
      $scope.users = User.query();
    });
  }
  
  $scope.updateUser = function(user) {
    user.$save(function(){
      $scope.users = User.query();
    });
  }
  
  $scope.deleteUser = function(id) {
    User.remove({"id": id}, function(){
      $scope.users = User.query();
    })
  }
}]); 