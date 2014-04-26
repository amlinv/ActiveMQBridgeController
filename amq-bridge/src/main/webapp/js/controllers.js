'use strict';

/* Controllers */

var amqBridgeApp = angular.module('amqBridgeApp', ['ngAnimate']);

amqBridgeApp.controller('amqBridgeCtrl', function($scope, $http) {
    $scope.connection_state = "initializing";

    $http.get(
        'http://localhost:8080/api/bridges', { "headers" : { "Accept" : "application/json" } }
    ).then (
        function(response) {
            $scope.bridges = response.data;
        }
        ,
        function(err) {
            /* Haven't found any useful error details in the argument(s) */
            $scope.note = "get bridge-list error";
        }
    );

    $scope.connection_state = "connecting";
    $scope.onAddBridge = function (upd) {
        if ( upd instanceof Array ) {
            $scope.bridges = $scope.bridges.concat(upd);
        } else {
            $scope.bridges.push(upd);
        }
    }

    $scope.onRemoveBridge = function (data) {
        var cur = $scope.bridges.length - 1;
        while ( cur >= 0 ) {
            if ( $scope.bridges[cur].id == data.id ) {
                $scope.bridges.splice(cur, 1);
            }
            cur--;
        }
    }

    $scope.onUpdateBridge = function (data) {
        // TBD
    }

    $scope.startBridge = function (id) {
        $scope.note = "requesting start of bridge " + id;
        var url = 'http://localhost:8080/api/bridges/' + id + '/start';

        $http.get(
            url, { "headers" : { "Accept" : "application/json" } }
        ).then (
            function(response) {
                $scope.note = "start bridge " + id + ": " + response.data;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error starting bridge " + id;
            }
        );
    }

    $scope.stopBridge = function (id) {
        $scope.note = "requesting stop of bridge " + id;
        var url = 'http://localhost:8080/api/bridges/' + id + '/stop';

        $http.get(
            url, { "headers" : { "Accept" : "application/json" } }
        ).then (
            function(response) {
                $scope.note = "stop bridge " + id + ": " + response.data;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error stopping bridge " + id;
            }
        );
    }

    try {
        var source = new WebSocket('ws://localhost:8080/ws/bridges');
    } catch ( exc ) {
        $scope.note = "websocket error";
    }

    source.onopen = function (event) {
        $scope.$apply(function() { $scope.connection_state = "connected" });
    }
    source.onmessage = function (event) {
        $scope.$apply(function() {
            var msg = JSON.parse(event.data);

            if ( msg.action == "add" ) {
                $scope.onAddBridge(msg.data);
            } else if ( msg.action == "remove" ) {
                $scope.onRemoveBridge(msg.data);
            } else if ( msg.action == "update" ) {
                $scope.onUpdateBridge(msg.data);
            }

            if ( $scope.debug_log ) {
                $scope.debug_log.append(event.data);
            }
        });
    }
    source.onerror = function (event) {
        $scope.$apply(
            function()
            {
                $scope.note = "websocket error";
            }
        )
    }
    source.onclose = function (event) {
        $scope.$apply(
            function()
            {
                $scope.connection_state = "disconnected";
            }
        );
    }

});
