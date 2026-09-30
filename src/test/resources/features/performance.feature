Feature: Post Management API - Performance Load Testing
  As a performance engineer
  I want to load test the Posts API under concurrent virtual users
  So that I can detect performance regressions before release

  Background:
    Given the API base URL is configured for the current environment

  @PerformanceTest
  Scenario: Execute a concurrent load test against the configured APIs
    Given the performance test is configured with threads, ramp-up and duration from system properties
    When virtual users concurrently invoke the selected APIs for the configured duration
    Then response time percentiles P50, P90, P95 and P99 should be calculated for each API
    And the average response time should not regress beyond the configured threshold
    And the error rate should not regress beyond the configured threshold
    And the throughput should not regress beyond the configured threshold
    And a performance report should be generated with the results
    And a performance trend report should be generated with the historical results

  @PerformanceTest @PerformanceSmoke
  Scenario: Execute a lightweight smoke-level load test with minimal load
    Given the performance test is configured with 2 threads, 5 seconds ramp-up and 15 seconds duration
    When virtual users concurrently invoke the selected APIs for the configured duration
    Then response time percentiles P50, P90, P95 and P99 should be calculated for each API
    And a performance report should be generated with the results
