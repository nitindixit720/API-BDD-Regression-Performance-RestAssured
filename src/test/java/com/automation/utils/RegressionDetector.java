package com.automation.utils;

import com.automation.config.ConfigManager;
import com.automation.model.PerformanceMetrics;
import com.automation.model.PerformanceRunRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares the current run's pooled metrics against the rolling average of the last N
 * historical runs sharing the same label (PerformanceHistoryManager), using the
 * configurable threshold percentages in config.properties. On the very first run for a
 * given label there is no history yet, so no regression is possible - that run simply
 * becomes the baseline.
 */
public final class RegressionDetector {

    private RegressionDetector() {
    }

    public static Result evaluate(PerformanceRunRecord current, List<PerformanceRunRecord> history) {
        if (history.isEmpty()) {
            return new Result(current.getLabel(), false, false, false,
                    List.of("No historical data available yet - this run establishes the baseline."));
        }

        PerformanceMetrics currentMetrics = current.getMetrics();
        double avgHistoricalResponseTime = history.stream().mapToDouble(r -> r.getMetrics().getAvgResponseTimeMs()).average().orElse(0);
        double avgHistoricalThroughput = history.stream().mapToDouble(r -> r.getMetrics().getThroughputPerSec()).average().orElse(0);
        double avgHistoricalErrorRate = history.stream().mapToDouble(r -> r.getMetrics().getErrorRatePercent()).average().orElse(0);

        ConfigManager config = ConfigManager.getInstance();
        List<String> reasons = new ArrayList<>();

        boolean responseTimeRegressed = exceedsThreshold(
                currentMetrics.getAvgResponseTimeMs(), avgHistoricalResponseTime,
                config.getRegressionResponseTimeThresholdPercent(), true);
        if (responseTimeRegressed) {
            reasons.add(String.format("Avg response time %.2fms exceeds baseline %.2fms by more than %.0f%%",
                    currentMetrics.getAvgResponseTimeMs(), avgHistoricalResponseTime, config.getRegressionResponseTimeThresholdPercent()));
        }

        boolean throughputRegressed = exceedsThreshold(
                currentMetrics.getThroughputPerSec(), avgHistoricalThroughput,
                config.getRegressionThroughputThresholdPercent(), false);
        if (throughputRegressed) {
            reasons.add(String.format("Throughput %.2f/s dropped below baseline %.2f/s by more than %.0f%%",
                    currentMetrics.getThroughputPerSec(), avgHistoricalThroughput, config.getRegressionThroughputThresholdPercent()));
        }

        boolean errorRateRegressed = currentMetrics.getErrorRatePercent() - avgHistoricalErrorRate > config.getRegressionErrorRateThresholdPercent();
        if (errorRateRegressed) {
            reasons.add(String.format("Error rate %.2f%% exceeds baseline %.2f%% by more than %.0f%%",
                    currentMetrics.getErrorRatePercent(), avgHistoricalErrorRate, config.getRegressionErrorRateThresholdPercent()));
        }

        return new Result(current.getLabel(), responseTimeRegressed, throughputRegressed, errorRateRegressed, reasons);
    }

    private static boolean exceedsThreshold(double current, double baseline, double thresholdPercent, boolean higherIsBad) {
        if (baseline == 0) {
            return false;
        }
        double deltaPercent = ((current - baseline) / baseline) * 100.0;
        return higherIsBad ? deltaPercent > thresholdPercent : deltaPercent < -thresholdPercent;
    }

    public static final class Result {
        private final String label;
        private final boolean responseTimeRegressed;
        private final boolean throughputRegressed;
        private final boolean errorRateRegressed;
        private final List<String> reasons;

        Result(String label, boolean responseTimeRegressed, boolean throughputRegressed,
               boolean errorRateRegressed, List<String> reasons) {
            this.label = label;
            this.responseTimeRegressed = responseTimeRegressed;
            this.throughputRegressed = throughputRegressed;
            this.errorRateRegressed = errorRateRegressed;
            this.reasons = reasons;
        }

        public boolean isRegressionDetected() {
            return responseTimeRegressed || throughputRegressed || errorRateRegressed;
        }

        public boolean isResponseTimeRegressed() {
            return responseTimeRegressed;
        }

        public boolean isThroughputRegressed() {
            return throughputRegressed;
        }

        public boolean isErrorRateRegressed() {
            return errorRateRegressed;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public String getLabel() {
            return label;
        }
    }
}
