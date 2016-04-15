var angular = require('angular');
require('angular-ui-router');

angular
  .module('map', [
    'ui.router'
  ])
  .config(function($stateProvider) {
      $stateProvider
        .state('home.map',{
          url: '/map',
          views: {
            'container': {
              templateUrl: './js/map/templates/map.html',
              controller: 'MapController as MapCtrl'
            }
          },
          resolve: {
            location: function($q){
              var defer = $q.defer();
              navigator.geolocation.getCurrentPosition(function(position){
                defer.resolve(position);
              })
              return defer.promise;
            },
            tour: function($q, $http){
              var defer = $q.defer();
              $http.get('/tour/locations').then(function(tour){
                defer.resolve(tour);
              });
              return defer.promise;
            }
          }
        })
  })
