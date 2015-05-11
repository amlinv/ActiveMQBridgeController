'use strict';

/* Controllers */

var amqBridgeApp = angular.module('amqBridgeApp', ['ngAnimate', 'custom-filters', 'angular-loading-bar']);

amqBridgeApp.controller('viewSelector', function($scope, $http) {
    $scope.view = 'bridges';

    $scope.showBridges = function() {
      $scope.view = 'bridges';
    };

    $scope.showMonitor = function() {
      $scope.view = 'monitor';
    };
});

amqBridgeApp.controller('amqBridgeCtrl', function($scope, $http) {
    $scope.connection_state = "initializing";
    $scope.log = { "messages": "" };
    $scope.statTimer = undefined;

    //// Default the opening view to the bridges view.
    //$scope.view = 'monitor';

    $scope.getBridgeList = function() {
        $http.get(
            'api/bridges', { "headers" : { "Accept" : "application/json" } }
        ).then (
            function(response) {
                $scope.updateBridgeList(response.data);
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "get bridge-list error";
            }
        );
    };

    $scope.startPeriodicStatRefresh = function (period) {
        if ( $scope.statTimer ) {
            clearInterval($scope.statTimer);
        }
        $scope.statTimer = setInterval(function() { $scope.refreshStats(); }, period);
    };

    $scope.sendCreateBridge = function(newBridge) {
        $scope.debug("Send create-bridge to server: " + JSON.stringify(newBridge));
        $http.put(
            'api/bridges/' + newBridge.id,
            JSON.stringify(newBridge),
            { "headers" : { "Accept" : "application/json", "Content-Type" : "application/json" } }
        ).then (
            function(response) {
                $scope.note = "bridge \"" + newBridge.id + "\" added";
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "create bridge \"" + newBridge.id + "\" error";
            }
        );
    };

    $scope.sendUpdateBridge = function(upd_bridge) {
        $scope.debug(JSON.stringify(upd_bridge));
        $http.post(
            'api/bridges/' + upd_bridge.id,
            JSON.stringify(upd_bridge),
            { "headers" : { "Accept" : "application/json", "Content-Type" : "application/json" } }
        ).then (
            function(response) {
                $scope.note = "bridge \"" + upd_bridge.id + "\" updated";
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "update bridge \"" + upd_bridge.id + "\" error";
            }
        );
    };

    $scope.sendDeleteBridge = function(id) {
        $http.delete(
            'api/bridges/' + id,
            { "headers" : { "Accept" : "application/json" } }
        ).then (
            function(response) {
                /* No need to remove from the table here; a websocket event will notify the UI when it's removed. */
                $scope.note = "bridge \"" + upd_bridge.id + "\" deleted";
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "delete bridge \"" + upd_bridge.id + "\" error";
            }
        );
    };

    $scope.sendStatsRequest = function() {
        $http.get(
            'api/bridges/stats',
            { "headers" : { "Accept" : "application/json" } }
        ).then (
            function(response) {
                $scope.updateStats(response.data);
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "update bridge error";
            }
        );
    };

    $scope.refreshStats = function () {
        /* TBD */
        $scope.sendStatsRequest();
    };

    $scope.onProcessOneBridge = function (bridge) {
        var bridge_copy = angular.copy(bridge);

        if ( ! ( 'newBridge' in bridge_copy ) ) {
            bridge_copy.newBridge = false;
        }

        var index = $scope.findBridgeIndexWithId(bridge.id);
        if ( index == -1 ) {
            //
            // Unknown ID - add a new one.
            //
            $scope.bridges.push(bridge_copy);
            $scope.bridgesSnapshot.push(angular.copy(bridge_copy));
        } else {
            //
            // Known ID - update the existing one.
            //
            $scope.bridges[index] = bridge_copy;
            $scope.bridgesSnapshot[index] = angular.copy(bridge_copy);
        }
    };

    $scope.onAddBridge = function (upd) {
        var copy = angular.copy(upd);

        if ( copy instanceof Array ) {
            var cur = 0;
            while ( cur < copy.length ) {
                $scope.onProcessOneBridge(copy[cur]);
                cur++;
            }
        } else {
            $scope.onProcessOneBridge(copy);
        }
    };

    /* TBD: more efficient solution */
    $scope.findBridgeIndexWithId = function (id) {
        /* TBD: use the array; need to use the "original" id */
        /* $scope.bridgeIndex[$scope.bridges[cur].id] = cur; */
        var cur = 0;

        while ( cur < $scope.bridges.length ) {
            if ( $scope.bridges[cur].id == id ) {
                return  cur;
            }
            cur++;
        }

        return  -1;
    };

    $scope.onRemoveBridge = function (data) {
        var cur = $scope.bridges.length - 1;
        while ( cur >= 0 ) {
            if ( $scope.bridges[cur].id == data.id ) {
                $scope.bridges.splice(cur, 1);
                $scope.bridgesSnapshot.splice(cur, 1);
            }
            cur--;
        }
    };

    $scope.onUpdateBridge = function (data) {
        // TBD
    };

    $scope.onBridgeEvent = function (data) {
        $scope.debug("BRIDGE EVENT: " + JSON.stringify(data));

        if ( data.type == "BRIDGE_STOPPED" ) {
            var index = $scope.findBridgeIndexWithId(data.data);
            if ( index != -1 ) {
                $scope.bridges[index].running = false;
            } else {
                $scope.note = "received stopped event for unknown bridge id '" + data.data + "'";
            }
        } else if ( data.type == "BRIDGE_STARTED" ) {
            var index = $scope.findBridgeIndexWithId(data.data);
            if ( index != -1 ) {
                $scope.bridges[index].running = true;
            } else {
                $scope.note = "received started event for unknown bridge id '" + data.data + "'";
            }
        }
    };

    $scope.onBridgeStats = function (data) {
        $scope.debug("BRIDGE STATS: " + JSON.stringify(data));
        if ( "bridgeId" in data ) {
            var index = $scope.findBridgeIndexWithId(data.bridgeId);
            if ( index != -1 ) {
                $scope.bridges[index].stats = data;
            } else {
                $scope.note = "received stats event for unknown bridge id '" + data.bridgeId + "'";
            }
        }
    };

    $scope.startBridge = function (id) {
        $scope.note = "requesting start of bridge " + id;
        var url = 'api/bridges/' + id + '/start';

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
    };

    $scope.stopBridge = function (id) {
        $scope.note = "requesting stop of bridge " + id;
        var url = 'api/bridges/' + id + '/stop';

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
    };

    try {
        var scheme;
        var host_and_port;
        var path;
        if (window.location.protocol == "https:") {
            scheme = "wss://";
        } else {
            scheme = "ws://";
        }
        host_and_port = window.location.host;
        path = window.location.pathname.replace(/[^/]*$/, "");

        var source = new WebSocket(scheme + host_and_port + path + "/ws/bridges");

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
                } else if ( msg.action == "bridgeEvent" ) {
                    $scope.onBridgeEvent(msg.data);
                } else if ( msg.action == "stats" ) {
                    $scope.onBridgeStats(msg.data);
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
    } catch ( exc ) {
        $scope.note = "websocket error";
    }

    $scope.matchBridge = function(actual, expected) {
        if ( actual ) {
            return ( actual['srcUrl'] == expected );
        }

        return  false;
    };

    $scope.updateBridgeList = function(newList) {
        $scope.bridges = newList;
        $scope.bridgesSnapshot = angular.copy($scope.bridges);

        $scope.bridgeIndex = {};

        var cur = 0;
        while ( cur < $scope.bridges.length ) {
            $scope.bridgeIndex[$scope.bridges[cur].id] = cur;
            cur++;
        }
    };

    $scope.updateStats = function (stats) {
        /* TBD: what will the data format be? Array of object with bridge ID and stats..... */
        var cur = 0;
        while ( cur < stats.length ) {
            $scope.onBridgeStats(stats[cur]);
            cur++;
        }
    };

    $scope.saveBridgeEdits = function(id) {
        var index = $scope.findBridgeIndexWithId(id);
        if ( index != -1 ) {
            var bridge = $scope.bridges[index];
            if ( ( typeof(bridge.queueList) == "string" ) || ( bridge.queueList instanceof String ) ) {
                bridge.queueList = bridge.queueList.split(",");
            }
            if ( ( typeof(bridge.topicList) == "string" ) || ( bridge.topicList instanceof String ) ) {
                bridge.topicList = bridge.topicList.split(",");
            }

            if ( ( 'newBridge' in bridge ) && ( bridge.newBridge ) ) {
                this.sendCreateBridge(bridge);
            } else {
                this.sendUpdateBridge(bridge);
            }

            $scope.bridgesSnapshot[index] = angular.copy($scope.bridges[index]);
        }
    };

    $scope.undoBridgeEdits = function(id) {
        var index = $scope.findBridgeIndexWithId(id);
        if ( index != -1 ) {
            if ( ( 'newBridge' in $scope.bridges[index] ) && ( $scope.bridges[index].newBridge ) ) {
                //
                // Cancel on adding a new bridge; remove it.
                //
                $scope.bridges.splice(index, 1);
                $scope.bridgesSnapshot.splice(index, 1);
            } else {
                //
                // Cancel on editting an existing bridge; revert.
                //
                $scope.bridges[index] = angular.copy($scope.bridgesSnapshot[index]);
            }
        }
    };

    $scope.deleteBridge = function(id) {
        var index = $scope.findBridgeIndexWithId(id);
        if ( index != -1 ) {
            $scope.sendDeleteBridge(id);
        } else {
            $scope.debug("failed to locate bridge \"" + id + "\" to delete");
        }
    };

    $scope.addNewBridge = function() {
        var randNum = Math.floor(Math.random() * 1000 ) % 1000;
        var newId   = "bridge-" + randNum;
        var bridge  =
            {
                "id": newId,
                "srcUrl": "failover://(tcp://localhost:61616)",
                "destUrl": "failover://(tcp://localhost:61626)",
                "queueList": ["QUEUE-A", "QUEUE-B"],
                "topicList": ["TOPIC-A", "TOPIC-B"],
                "running": false,
                "editMode": true,
                "newBridge": true
            };

        this.onAddBridge(bridge);
    };

    $scope.startEdit = function(id) {
        var index = $scope.findBridgeIndexWithId(id);
        if ( index != -1 ) {
            $scope.bridges[index].editMode = true;
        }
    };

    /**
     * DEBUGGING
     */
    $scope.debug = function(msg) {
        $scope.log.messages = $scope.log.messages.concat(msg) + "\n";
    };

    /**
     * Get the bridge list now.
     */
    $scope.getBridgeList();

    $scope.connection_state = "connecting";

    /**
     * TBD: convert to using the Angular method of periodic refresh.
     */
    // TBD999: is this needed with the websocket?
    //$scope.startPeriodicStatRefresh(3000);
});


//
// MONITOR
//
amqBridgeApp.controller('amqMonitor', function($scope, $http) {
    $scope.debug_log = "Start of debug log. ";
    $scope.connection_state = "monitor initializing";

    $scope.monitoredBrokers = [];
    $scope.queueStats = [];

    try {
        var scheme;
        var host_and_port;
        var path;
        if (window.location.protocol == "https:") {
            scheme = "wss://";
        } else {
            scheme = "ws://";
        }
        host_and_port = window.location.host;
        path = window.location.pathname.replace(/[^/]*$/, "");

        var source = new WebSocket(scheme + host_and_port + path + "/ws/monitor");

        source.onopen = function (event) {
            $scope.$apply(function() { $scope.connection_state = "monitor connected" });
        };
        source.onmessage = function (event) {
            $scope.$apply(function() {
                var msg = JSON.parse(event.data);

                if ( msg.action == "brokerStats" ) {
                    $scope.onMonitorBrokerStats(msg.data);
                } else if ( msg.action == "queueStats" ) {
                    $scope.onMonitorQueueStats(msg.data);
                }

                //if ( $scope.debug_log ) {
                    $scope.debug_log = event.data;
                //}
            });
        };
        source.onerror = function (event) {
            $scope.$apply(
                function()
                {
                    $scope.note = "websocket error";
                }
            )
        };

        source.onclose = function (event) {
            $scope.$apply(
                function()
                {
                    $scope.connection_state = "monitor disconnected";
                }
            );
        }
    } catch ( exc ) {
        $scope.note = "websocket error";
    }

    $scope.onMonitorBrokerStats = function(stats) {
        if ( ( stats ) && ( stats.brokerStats ) && ( stats.brokerStats.brokerName ) ) {
            var name = stats.brokerStats.brokerName;
            var index;
            if ( name in $scope.monitoredBrokers ) {
                index = $scope.monitoredBrokers[name];
            } else {
                index = $scope.monitoredBrokers.length;
                $scope.monitoredBrokers[name] = index;
            }

            $scope.monitoredBrokers[index] = stats;
        }
    };

    $scope.onMonitorQueueStats = function(stats) {
        var updatedQueueStats = [];

        if ( stats ) {
            var count = 0;
            for ( var queueName in stats ) {
                updatedQueueStats[count] = stats[queueName];
                updatedQueueStats[count]['queueName'] = queueName;
                count++;
            }
        }

        $scope.queueStats = updatedQueueStats;
    };

    $scope.addMonitorBroker = function() {
        var spec = prompt("Please specify the broker location and broker name in the format " +
                          "broker-name/location; broker-name is optional", "broker/location");

        if ( spec != "" ) {
            var brokerName = "*";
            var brokerLocation = "";

            var pos = spec.indexOf("/");
            if ( pos != -1 ) {
                brokerName = spec.substr(0, pos);
                brokerLocation = spec.substr(pos + 1);
            } else {
                brokerLocation = spec;
            }

            $scope.sendAddMonitorBroker(brokerName, brokerLocation);
        }
    };

    $scope.removeMonitorBroker = function() {
        var spec = prompt("Please specify the broker location and broker name in the format " +
                          "broker-name/location; broker-name is optional", "broker/location");

        if ( spec != "" ) {
            var brokerName = "*";
            var brokerLocation = "";

            var pos = spec.indexOf("/");
            if ( pos != -1 ) {
                brokerName = spec.substr(0, pos);
                brokerLocation = spec.substr(pos + 1);
            } else {
                brokerLocation = spec;
            }

            $scope.sendRemoveMonitorBroker(brokerName, brokerLocation);
        }
    };

    $scope.addMonitorQueue = function() {
        var queueName = prompt("Please specify the queue to monitor, or * to add all known queues");

        if ( queueName != "" ) {
            $scope.sendAddMonitorQueue(queueName);
        }
    };

    $scope.removeMonitorQueue = function() {
        var queueName = prompt("Please specify the queue to remove, or * to remove all known queues");

        if ( queueName != "" ) {
            $scope.sendRemoveMonitorQueue(queueName);
        }
    };

    $scope.startMonitor = function() {
        $scope.sendStartMonitor();
    };

    $scope.sendAddMonitorBroker = function(brokerName, brokerLocation) {
        var requestBody = {
            "brokerName": brokerName,
            "address" : brokerLocation
        };

        //$scope.debug("Send add-monitor-broker to server: " + JSON.stringify(requestBody));
        $http.put(
            'api/monitor/broker/',
            $.param(requestBody),
            { "headers" : { "Accept" : "application/json", 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then (
            function(response) {
                $scope.note = "added broker " + brokerName + "/" + brokerLocation;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error adding broker " + brokerName + "/" + brokerLocation;
            }
        );
    };

    $scope.sendRemoveMonitorBroker = function(brokerName, brokerLocation) {
        var requestBody = {
            "brokerName": brokerName,
            "address" : brokerLocation
        };

        $http.delete(
            'api/monitor/broker/',
            { params: requestBody }
        ).then (
            function(response) {
                $scope.note = "removed broker " + brokerName + "/" + brokerLocation;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error on remove broker " + brokerName + "/" + brokerLocation;
            }
        );
    };

    $scope.sendAddMonitorQueue = function(queueName) {
        var requestBody = {
            "queueName": queueName
        };

        //$scope.debug("Send add-monitor-broker to server: " + JSON.stringify(requestBody));
        $http.put(
            'api/monitor/queue',
            $.param(requestBody),
            { "headers" : { 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then (
            function(response) {
                $scope.note = "added queue " + queueName;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error adding queue \"" + queueName;
            }
        );
    };

    $scope.sendRemoveMonitorQueue = function(queueName) {
        var requestBody = {
            "queueName": queueName
        };

        $http.delete(
            'api/monitor/queue',
            { params: requestBody }
        ).then (
            function(response) {
                $scope.note = "removed queue " + queueName;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error on remove queue " + queueName;
            }
        );
    };

    $scope.sendStartMonitor = function() {
        $http.get(
            'api/monitor/start'
        ).then (
            function(response) {
                $scope.note = "monitor started";
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "failed to start monitor";
            }
        );
    };
});
