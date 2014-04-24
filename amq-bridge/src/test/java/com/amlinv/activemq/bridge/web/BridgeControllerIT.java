package com.amlinv.activemq.bridge.web;

import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import com.amlinv.activemq.bridge.model.impl.AmqBridgeSpecImpl;
import cucumber.annotation.en.And;
import cucumber.annotation.en.Then;
import cucumber.runtime.PendingException;

/**
 * Created by art on 4/20/14.
 * TBD999: move this to src/it after figuring out how to build and run tests from there
 */
public class BridgeControllerIT {
    private AmqBridgeSpec bridge;

    @cucumber.annotation.en.Given("^bridgeController with no bridges$")
    public void bridgeController_with_no_bridges() throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new cucumber.runtime.PendingException();
    }

    @And("^bridge from \"([^\"]*)\" to \"([^\"]*)\" named \"([^\"]*)\"$")
    public void bridge_from_to_named(String src, String dest, String name) throws Throwable {
        this.bridge = new AmqBridgeSpecImpl();
        this.bridge.setId(name);
        this.bridge.setSrcUrl(src);
        this.bridge.setDestUrl(dest);
    }

    @Then("^bridge is added to the controller's bridge list$")
    public void bridge_is_added_to_the_controller_s_bridge_list() throws Throwable {
        // TBD999: need a REST client here
//        Endpoint endpt = new EndpointImpl()
//        ClientImpl client = new ClientImpl();

        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }
}

//@RunWith(Cucumber.class)
//
//@CucumberOptions(  monochrome = true,
//        tags = "@tags",
//        features = "src/it/com/amlinv/activemq/bridge/BridgeController.feature",
//        format = { "pretty","html: cucumber-html-reports",
//                "json: cucumber-html-reports/cucumber.json" },
//        dryRun = false,
//        glue = "com.amlinv.activemq.bridge")
