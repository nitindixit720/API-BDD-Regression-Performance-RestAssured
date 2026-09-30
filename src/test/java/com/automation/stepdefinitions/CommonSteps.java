package com.automation.stepdefinitions;

import com.automation.config.ConfigManager;
import com.automation.utils.LoggerUtil;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;

/**
 * Steps shared by both functional.feature and performance.feature backgrounds.
 * Kept in its own class so the step text is defined exactly once (Cucumber throws an
 * "ambiguous step definitions" error if the same pattern exists in two glue classes).
 */
public class CommonSteps {

    private static final Logger log = LoggerUtil.getLogger(CommonSteps.class);

    @Given("the API base URL is configured for the current environment")
    public void the_api_base_url_is_configured() {
        ConfigManager config = ConfigManager.getInstance();
        log.info("Environment: {} | Base URL: {}", config.getEnvironment(), config.getBaseUrl());
    }
}
