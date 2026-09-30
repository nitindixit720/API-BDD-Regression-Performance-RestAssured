package com.automation.utils;

import com.automation.model.PerformanceRunRecord;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists each run's PerformanceRunRecord to a rolling CSV history file so subsequent runs
 * can be compared against a baseline (see RegressionDetector) and rendered as a trend line
 * (see PerformanceTrendReportGenerator). Kept as a flat CSV rather than a database to keep
 * the framework dependency-free and CI-artifact friendly.
 *
 * Deliberately stored under src/test/resources (alongside the trend report itself) rather
 * than target/ - target/ is deleted by "mvn clean", which would otherwise reset the trend
 * to a fresh single-run baseline on every clean build. Keeping it here means the trend
 * genuinely accumulates across CI runs, and the history file can be committed to source
 * control alongside the reports it drives.
 */
public final class PerformanceHistoryManager {

    private static final Logger log = LoggerUtil.getLogger(PerformanceHistoryManager.class);
    private static final Path HISTORY_FILE = Paths.get(
            "src", "test", "resources", "reports", "performance-trend", "performance_history.csv");

    private PerformanceHistoryManager() {
    }

    public static synchronized void record(PerformanceRunRecord run) {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            boolean append = Files.exists(HISTORY_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(HISTORY_FILE,
                    append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE)) {
                writer.write(run.toCsvRow());
                writer.newLine();
            }
        } catch (IOException e) {
            log.warn("Unable to persist performance history for {}: {}", run.getLabel(), e.getMessage());
        }
    }

    public static synchronized List<PerformanceRunRecord> getRecentHistory(String label, int limit) {
        List<PerformanceRunRecord> matches = new ArrayList<>();
        if (!Files.exists(HISTORY_FILE)) {
            return matches;
        }
        try (BufferedReader reader = Files.newBufferedReader(HISTORY_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(label + ",")) {
                    matches.add(PerformanceRunRecord.fromCsvRow(line));
                }
            }
        } catch (IOException e) {
            log.warn("Unable to read performance history: {}", e.getMessage());
        }
        int size = matches.size();
        return size <= limit ? matches : matches.subList(size - limit, size);
    }
}
