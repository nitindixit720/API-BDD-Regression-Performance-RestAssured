package com.automation.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed scenario up to MAX_RETRY_COUNT times before it is marked failed, to absorb
 * transient network issues against remote APIs. Applied automatically via RetryTransformer.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY_COUNT = 2;
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            return true;
        }
        return false;
    }
}
