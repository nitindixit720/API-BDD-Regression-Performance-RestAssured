package com.automation.model;

/**
 * A single request's result during a load test run, timestamped relative to the run's
 * start. Pooled samples drive the time-bucketed charts (response time / throughput /
 * errors over time) and the response-time distribution histogram in the performance report.
 */
public final class PerformanceSample {

    private final long elapsedMs;
    private final long latencyMs;
    private final boolean success;
    private final String apiName;

    public PerformanceSample(long elapsedMs, long latencyMs, boolean success, String apiName) {
        this.elapsedMs = elapsedMs;
        this.latencyMs = latencyMs;
        this.success = success;
        this.apiName = apiName;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getApiName() {
        return apiName;
    }
}
