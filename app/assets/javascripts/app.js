var mod = angular.module("app", ['ngResource','ngMap']);

mod.controller("MainCtrl", ["$scope","$resource", function($scope, $resource){
  var Session = $resource('/api/sessions/:id', {id:'@session'});
  var SessionData = $resource('/api/sessiondata/:id', {id:'@id'});
  
  
  $scope.mapStartMarker = new google.maps.Marker({title: "Start Marker"});
  $scope.mapEndMarker = new google.maps.Marker({title: "End Marker"});
  $scope.mapEndMarker.setIcon("https://developers.google.com/maps/documentation/javascript/examples/full/images/beachflag.png");
  
  $scope.sessions = Session.query();
  
  $scope.selectSession = function(sess) {
    $scope.selectedSession = sess;
  }
  
  $scope.applyStartEndTime = function(){
    var data = SessionData.get({id: $scope.selectedSession, startTime: $scope.selectedStartTime, endTime: $scope.selectedEndTime}, function(){
      $scope.selectedStartTime = data.start;
      $scope.selectedEndTime = data.end;
      console.log($scope.selectedStartTime);
    });
    $scope.currentData = data;
  }
  
  $scope.$watch("selectedSession", function(){
    //console.log("selectedSession = " + $scope.selectedSession);
    if($scope.selectedSession) {
      var data = SessionData.get({id: $scope.selectedSession}, function(){
        $scope.selectedStartTime = data.start;
        $scope.selectedEndTime = data.end;
        //console.log($scope.selectedStartTime);
      });
      $scope.currentData = data;
    }
  });
  
  $scope.$watch("selectedStartTime", function(){
    if($scope.selectedStartTime) {
      var lat = $scope.currentData.startLoc.lat;
      var lng = $scope.currentData.startLoc.lng;
      var loc = new google.maps.LatLng(lat, lng);
      $scope.mapStartMarker.setPosition(loc);
      $scope.mapStartMarker.setMap($scope.map);
      $scope.mapStartMarker.setVisible(true);

      var lat1 = $scope.currentData.endLoc.lat;
      var lng1 = $scope.currentData.endLoc.lng;
      var loc1 = new google.maps.LatLng(lat1, lng1);
      $scope.mapEndMarker.setPosition(loc1);
      $scope.mapEndMarker.setMap($scope.map);
      $scope.mapEndMarker.setVisible(true);

    } else {
      $scope.mapStartMarker.setVisible(false);
      $scope.mapEndMarker.setVisible(false);
    }
  });
  
  $scope.hasSessionName = function(sess){
    var flag = sess.sessionName != undefined && sess.sessionName != ""; 
    return flag;
  }
  
  $scope.updateSessionName = function(sess) {
    console.log(sess);
    sess.$save(function(){
      $scope.sessions = Session.query();
    });
  }
  
  $scope.deleteSelectedSession = function(sessionId) {
    Session.remove({session: sessionId}, function(){
      $scope.selectedSession = undefined;
      $scope.sessions = Session.query();
    });
  } 
  
}]);