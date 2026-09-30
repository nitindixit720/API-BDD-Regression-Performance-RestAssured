package com.automation.config;

import java.util.Arrays;
import java.util.List;

/**
 * Reads performance test parameters from system properties so load profile can be tuned
 * per run without code changes, e.g.:
 * mvn clean test -Dthreads=25 -DrampUp=30 -Dduration=1800 -Dapis=Get-Posts,Get-Post-By-Id
 */
public final class PerformanceConfigManager {

    private static volatile PerformanceConfigManager instance;

    private static final List<String> ALL_APIS =
            List.of("Get-Posts", "Get-Post-By-Id", "Get-Comments", "Create-Post");

    private final int threads;
    private final int rampUpSeconds;
    private final int durationSeconds;
    private final List<String> selectedApis;

    private PerformanceConfigManager() {
        this.threads = Integer.parseInt(System.getProperty("threads", "10"));
        this.rampUpSeconds = Integer.parseInt(System.getProperty("rampUp", "30"));
        this.durationSeconds = Integer.parseInt(System.getProperty("duration", "120"));

        String apis = System.getProperty("apis", "ALL");
        this.selectedApis = "ALL".equalsIgnoreCase(apis)
                ? ALL_APIS
                : Arrays.asList(apis.split("\\s*,\\s*"));
    }

    public static PerformanceConfigManager getInstance() {
        if (instance == null) {
            synchronized (PerformanceConfigManager.class) {
                if (instance == null) {
                    instance = new PerformanceConfigManager();
                }
            }
        }
        return instance;
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

    public List<String> getSelectedApis() {
        return selectedApis;
    }
}
