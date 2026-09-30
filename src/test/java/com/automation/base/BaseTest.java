package com.automation.base;

import com.automation.client.ApiClient;

/**
 * Shared test-layer base. Kept intentionally thin - step definitions instantiate their own
 * ApiClient, but extend this when a suite needs shared setup beyond what Hooks provides.
 */
public class BaseTest {
    protected static final ApiClient API_CLIENT = new ApiClient();
}
