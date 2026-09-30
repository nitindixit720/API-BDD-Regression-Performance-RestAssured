package com.automation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable summary of one load test run's pooled results: latency percentiles, throughput
 * and error rate across every sample recorded during the run (see PerformanceSample).
 */
public final class PerformanceMetrics {

    private final String label;
    private final int totalRequests;
    private final int successCount;
    private final int failureCount;
    private final double avgResponseTimeMs;
    private final long minResponseTimeMs;
    private final long maxResponseTimeMs;
    private final long p50;
    private final long p75;
    private final long p90;
    private final long p95;
    private final long p99;
    private final double throughputPerSec;
    private final double errorRatePercent;

    PerformanceMetrics(String label, int totalRequests, int successCount, int failureCount,
                        double avgResponseTimeMs, long minResponseTimeMs, long maxResponseTimeMs,
                        long p50, long p75, long p90, long p95, long p99,
                        double throughputPerSec, double errorRatePercent) {
        this.label = label;
        this.totalRequests = totalRequests;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.avgResponseTimeMs = avgResponseTimeMs;
        this.minResponseTimeMs = minResponseTimeMs;
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.p50 = p50;
        this.p75 = p75;
        this.p90 = p90;
        this.p95 = p95;
        this.p99 = p99;
        this.throughputPerSec = throughputPerSec;
        this.errorRatePercent = errorRatePercent;
    }

    public static PerformanceMetrics compute(String label, List<Long> responseTimesMs,
                                              int successCount, int failureCount, int durationSeconds) {
        List<Long> sorted = new ArrayList<>(responseTimesMs);
        Collections.sort(sorted);

        int total = sorted.size();
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = sorted.isEmpty() ? 0 : sorted.get(0);
        long max = sorted.isEmpty() ? 0 : sorted.get(total - 1);
        double errorRate = total == 0 ? 0 : (failureCount * 100.0) / total;
        double throughput = durationSeconds == 0 ? 0 : (double) total / durationSeconds;

        return new PerformanceMetrics(
                label, total, successCount, failureCount, avg, min, max,
                percentile(sorted, 50), percentile(sorted, 75), percentile(sorted, 90),
                percentile(sorted, 95), percentile(sorted, 99),
                throughput, errorRate
        );
    }

    private static long percentile(List<Long> sorted, int percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public String getLabel() {
        return label;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public double getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public long getP50() {
        return p50;
    }

    public long getP75() {
        return p75;
    }

    public long getP90() {
        return p90;
    }

    public long getP95() {
        return p95;
    }

    public long getP99() {
        return p99;
    }

    public double getThroughputPerSec() {
        return throughputPerSec;
    }

    public double getErrorRatePercent() {
        return errorRatePercent;
    }

    @Override
    public String toString() {
        return String.format(
                "total=%d, success=%d, failed=%d, avg=%.2fms, p95=%dms, throughput=%.2f/s, errorRate=%.2f%%",
                totalRequests, successCount, failureCount, avgResponseTimeMs, p95, throughputPerSec, errorRatePercent);
    }
}
