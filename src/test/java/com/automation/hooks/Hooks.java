package com.automation.hooks;

import com.automation.utils.LoggerUtil;
import com.automation.utils.PayloadStore;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;

public class Hooks {

    private static final Logger log = LoggerUtil.getLogger(Hooks.class);

    @Before
    public void beforeScenario(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());
    }

    @After
    public void afterScenario(Scenario scenario) {
        log.info("Finished scenario: {} - Status: {}", scenario.getName(), scenario.getStatus());
        PayloadStore.clear();
    }
}
