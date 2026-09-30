package com.automation.utils;

import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Registered in testng.xml as a &lt;listener&gt;. Cucumber's own "json:" plugin only
 * flushes cucumber.json to disk when the Cucumber test run tears down (which happens
 * inside AbstractTestNGCucumberTests' internal @AfterClass, itself triggered by TestNG
 * finishing that test class) - that is later than Cucumber's own @AfterAll hook fires, so
 * generating the Regression Report from a @AfterAll hook reads an empty/incomplete file.
 * ISuiteListener#onFinish runs after every class in the suite (including that teardown)
 * has completed, which is the earliest point the JSON file is guaranteed to be complete.
 */
public class RegressionReportSuiteListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        // no-op
    }

    @Override
    public void onFinish(ISuite suite) {
        RegressionReportGenerator.generate();
    }
}
