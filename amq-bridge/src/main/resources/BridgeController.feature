Feature: controlBridges

  Scenario add a new bridge
    Given bridgeController with no bridges
    And bridge from "tcp://broker-5.4:61616" to "tcp://broker-5.9:61616" named "5.4-to-5.9-queueA"
    When bridge add operation is executed
    Then bridge is added to the controller's bridge list

  Scenario list bridges
    Given bridgeController with bridge named "5.4-to-5.9-queueA"
