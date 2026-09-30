package com.automation.config;

import java.util.Properties;

/**
 * Environment-aware configuration. Environment is selected via the "-Denv" system property
 * (defaults to DEV) and resolves to config/config-{ENV}.properties, layered on top of the
 * shared defaults in config/config.properties.
 */
public final class ConfigManager {

    private static volatile ConfigManager instance;

    private final String environment;
    private final Properties properties = new Properties();

    private ConfigManager() {
        this.environment = System.getProperty("env", "DEV").toUpperCase();
        properties.putAll(PropertiesLoader.load("config/config.properties"));
        properties.putAll(PropertiesLoader.load("config/config-" + environment + ".properties"));
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        return properties.getProperty("base.url");
    }

    public boolean isAuthRequired() {
        return Boolean.parseBoolean(properties.getProperty("auth.required", "false"));
    }

    public String getBearerToken() {
        // Environment variable takes precedence so CI/CD can inject secrets without editing files.
        String envToken = System.getenv("API_BEARER_TOKEN");
        return envToken != null ? envToken : properties.getProperty("bearer.token", "");
    }

    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("connection.timeout", "10000"));
    }

    public int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("socket.timeout", "10000"));
    }

    public double getRegressionResponseTimeThresholdPercent() {
        return Double.parseDouble(properties.getProperty("regression.threshold.responsetime.percent", "20"));
    }

    public double getRegressionThroughputThresholdPercent() {
        return Double.parseDouble(properties.getProperty("regression.threshold.throughput.percent", "15"));
    }

    public double getRegressionErrorRateThresholdPercent() {
        return Double.parseDouble(properties.getProperty("regression.threshold.errorrate.percent", "5"));
    }

    public int getPerformanceHistorySize() {
        return Integer.parseInt(properties.getProperty("performance.history.size", "5"));
    }

    public double getSlaMaxErrorRatePercent() {
        return Double.parseDouble(properties.getProperty("performance.sla.errorrate.max.percent", "20"));
    }

    public double getSlaMinThroughputPerSec() {
        return Double.parseDouble(properties.getProperty("performance.sla.throughput.min", "1"));
    }

    public double getSlaMaxAvgResponseTimeMs() {
        return Double.parseDouble(properties.getProperty("performance.sla.avgresponse.max.ms", "500"));
    }

    public double getSlaMaxP90ResponseTimeMs() {
        return Double.parseDouble(properties.getProperty("performance.sla.p90.max.ms", "500"));
    }
}
