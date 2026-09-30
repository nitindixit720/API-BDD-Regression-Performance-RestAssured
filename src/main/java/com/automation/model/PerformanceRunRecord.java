package com.automation.model;

/**
 * One load-test run's context (when/where/how it was run) plus its pooled PerformanceMetrics.
 * Persisted to the history CSV (see PerformanceHistoryManager) so the trend report and
 * regression detector can compare a run against prior runs sharing the same label.
 */
public final class PerformanceRunRecord {

    private final String label;
    private final String timestamp;
    private final String environment;
    private final int threads;
    private final int rampUpSeconds;
    private final int durationSeconds;
    private final PerformanceMetrics metrics;

    public PerformanceRunRecord(String label, String timestamp, String environment,
                                 int threads, int rampUpSeconds, int durationSeconds,
                                 PerformanceMetrics metrics) {
        this.label = label;
        this.timestamp = timestamp;
        this.environment = environment;
        this.threads = threads;
        this.rampUpSeconds = rampUpSeconds;
        this.durationSeconds = durationSeconds;
        this.metrics = metrics;
    }

    public String getLabel() {
        return label;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public int getThreads() {
        return threads;
    }

    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    public String toCsvRow() {
        return String.join(",",
                label,
                timestamp,
                environment,
                String.valueOf(threads),
                String.valueOf(rampUpSeconds),
                String.valueOf(durationSeconds),
                String.valueOf(metrics.getTotalRequests()),
                String.valueOf(metrics.getSuccessCount()),
                String.valueOf(metrics.getFailureCount()),
                String.valueOf(metrics.getAvgResponseTimeMs()),
                String.valueOf(metrics.getMinResponseTimeMs()),
                String.valueOf(metrics.getMaxResponseTimeMs()),
                String.valueOf(metrics.getP50()),
                String.valueOf(metrics.getP75()),
                String.valueOf(metrics.getP90()),
                String.valueOf(metrics.getP95()),
                String.valueOf(metrics.getP99()),
                String.valueOf(metrics.getThroughputPerSec()),
                String.valueOf(metrics.getErrorRatePercent()));
    }

    public static PerformanceRunRecord fromCsvRow(String row) {
        String[] p = row.split(",", -1);
        PerformanceMetrics metrics = new PerformanceMetrics(
                p[0],
                Integer.parseInt(p[6]),
                Integer.parseInt(p[7]),
                Integer.parseInt(p[8]),
                Double.parseDouble(p[9]),
                Long.parseLong(p[10]),
                Long.parseLong(p[11]),
                Long.parseLong(p[12]),
                Long.parseLong(p[13]),
                Long.parseLong(p[14]),
                Long.parseLong(p[15]),
                Long.parseLong(p[16]),
                Double.parseDouble(p[17]),
                Double.parseDouble(p[18])
        );
        return new PerformanceRunRecord(
                p[0], p[1], p[2],
                Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                metrics);
    }
}
