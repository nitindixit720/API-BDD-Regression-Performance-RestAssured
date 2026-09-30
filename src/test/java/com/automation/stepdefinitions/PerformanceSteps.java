package com.automation.stepdefinitions;

import com.automation.client.ApiClient;
import com.automation.config.ConfigManager;
import com.automation.config.PerformanceConfigManager;
import com.automation.constants.EndPoints;
import com.automation.model.PerformanceMetrics;
import com.automation.model.PerformanceRunRecord;
import com.automation.model.PerformanceSample;
import com.automation.utils.LoggerUtil;
import com.automation.utils.PerformanceHistoryManager;
import com.automation.utils.PerformanceReportGenerator;
import com.automation.utils.PerformanceTrendReportGenerator;
import com.automation.utils.RegressionDetector;
import com.automation.utils.TestDataProvider;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

/**
 * Multi-threaded load test step definitions (performance.feature). Spins up a fixed thread
 * pool sized to the configured virtual-user count, staggers thread start over the ramp-up
 * window, and has each thread repeatedly cycle through the selected APIs until the
 * configured duration elapses. Every individual request is recorded as a PerformanceSample
 * (elapsed offset, latency, success) so reports can render time-series charts, not just
 * end-of-run totals.
 */
public class PerformanceSteps {

    private static final Logger log = LoggerUtil.getLogger(PerformanceSteps.class);
    private static final List<Integer> POST_IDS = TestDataProvider.loadPostIds();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String[]> API_ENDPOINTS = new LinkedHashMap<>();

    static {
        API_ENDPOINTS.put("Get-Posts", new String[]{"GET", EndPoints.POSTS});
        API_ENDPOINTS.put("Get-Post-By-Id", new String[]{"GET", EndPoints.POST_BY_ID});
        API_ENDPOINTS.put("Get-Comments", new String[]{"GET", EndPoints.POST_COMMENTS});
        API_ENDPOINTS.put("Create-Post", new String[]{"POST", EndPoints.POSTS});
    }

    private int threads;
    private int rampUpSeconds;
    private int durationSeconds;
    private List<String> selectedApis;
    private PerformanceRunRecord currentRun;
    private RegressionDetector.Result regressionResult;
    private List<PerformanceSample> samples;

    @Given("the performance test is configured with threads, ramp-up and duration from system properties")
    public void configure_from_system_properties() {
        PerformanceConfigManager perfConfig = PerformanceConfigManager.getInstance();
        threads = perfConfig.getThreads();
        rampUpSeconds = perfConfig.getRampUpSeconds();
        durationSeconds = perfConfig.getDurationSeconds();
        selectedApis = perfConfig.getSelectedApis();
        log.info("Performance run configured: threads={}, rampUp={}s, duration={}s, apis={}",
                threads, rampUpSeconds, durationSeconds, selectedApis);
    }

    @Given("the performance test is configured with {int} threads, {int} seconds ramp-up and {int} seconds duration")
    public void configure_explicit(int threadCount, int rampUp, int duration) {
        this.threads = threadCount;
        this.rampUpSeconds = rampUp;
        this.durationSeconds = duration;
        this.selectedApis = PerformanceConfigManager.getInstance().getSelectedApis();
        log.info("Performance smoke run configured: threads={}, rampUp={}s, duration={}s, apis={}",
                threads, rampUpSeconds, durationSeconds, selectedApis);
    }

    @When("virtual users concurrently invoke the selected APIs for the configured duration")
    public void run_load_test() throws InterruptedException {
        ConcurrentLinkedQueue<PerformanceSample> collected = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(threads, 1));
        long testStartMillis = System.currentTimeMillis();
        long endTimeMillis = testStartMillis + (durationSeconds * 1000L);
        long rampUpIntervalMillis = threads > 0 ? (rampUpSeconds * 1000L) / threads : 0;
        ApiClient apiClient = new ApiClient();

        for (int i = 0; i < threads; i++) {
            long startDelay = i * rampUpIntervalMillis;
            executor.submit(() -> {
                try {
                    Thread.sleep(startDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (System.currentTimeMillis() < endTimeMillis) {
                    for (String api : selectedApis) {
                        long callStart = System.currentTimeMillis();
                        boolean success = invokeApi(apiClient, api);
                        long latency = System.currentTimeMillis() - callStart;
                        collected.add(new PerformanceSample(callStart - testStartMillis, latency, success, api));
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(durationSeconds + rampUpSeconds + 30L, TimeUnit.SECONDS);

        samples = new ArrayList<>(collected);
        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        for (PerformanceSample s : samples) {
            latencies.add(s.getLatencyMs());
            if (s.isSuccess()) successCount++;
            else failureCount++;
        }

        String label = resolveLabel();
        PerformanceMetrics metrics = PerformanceMetrics.compute(label, latencies, successCount, failureCount, durationSeconds);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        currentRun = new PerformanceRunRecord(label, timestamp, ConfigManager.getInstance().getEnvironment(),
                threads, rampUpSeconds, durationSeconds, metrics);

        log.info("Aggregate metrics for [{}]: {}", label, metrics);
    }

    private String resolveLabel() {
        if (selectedApis.size() == API_ENDPOINTS.size() && selectedApis.containsAll(API_ENDPOINTS.keySet())) {
            return "Post Management API - ALL";
        }
        return "Post Management API - " + String.join("+", selectedApis);
    }

    private boolean invokeApi(ApiClient apiClient, String apiName) {
        try {
            switch (apiName) {
                case "Get-Posts":
                    return apiClient.get(EndPoints.POSTS).getStatusCode() == 200;
                case "Get-Post-By-Id":
                    return apiClient.get(EndPoints.POST_BY_ID, randomPostId()).getStatusCode() == 200;
                case "Get-Comments":
                    return apiClient.get(EndPoints.POST_COMMENTS, randomPostId()).getStatusCode() == 200;
                case "Create-Post":
                    Map<String, Object> body = new HashMap<>();
                    body.put("title", "perf-test");
                    body.put("body", "load-test-payload");
                    body.put("userId", 1);
                    return apiClient.post(EndPoints.POSTS, body).getStatusCode() == 201;
                default:
                    log.warn("Unknown API selection: {}", apiName);
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private int randomPostId() {
        return POST_IDS.get(ThreadLocalRandom.current().nextInt(POST_IDS.size()));
    }

    @Then("response time percentiles P50, P90, P95 and P99 should be calculated for each API")
    public void percentiles_calculated() {
        assertTrue(currentRun != null, "No performance metrics were collected");
        assertTrue(currentRun.getMetrics().getTotalRequests() > 0, "No requests were recorded during the run");
    }

    @Then("the average response time should not regress beyond the configured threshold")
    public void check_response_time_regression() {
        ensureRegressionEvaluated();
        if (regressionResult.isResponseTimeRegressed()) {
            log.warn("[{}] Response time regression: {}", regressionResult.getLabel(), regressionResult.getReasons());
        }
    }

    @Then("the error rate should not regress beyond the configured threshold")
    public void check_error_rate_regression() {
        ensureRegressionEvaluated();
        if (regressionResult.isErrorRateRegressed()) {
            log.warn("[{}] Error rate regression: {}", regressionResult.getLabel(), regressionResult.getReasons());
        }
    }

    @Then("the throughput should not regress beyond the configured threshold")
    public void check_throughput_regression() {
        ensureRegressionEvaluated();
        if (regressionResult.isThroughputRegressed()) {
            log.warn("[{}] Throughput regression: {}", regressionResult.getLabel(), regressionResult.getReasons());
        }
    }

    private void ensureRegressionEvaluated() {
        if (regressionResult != null) {
            return;
        }
        int historySize = ConfigManager.getInstance().getPerformanceHistorySize();
        List<PerformanceRunRecord> history = PerformanceHistoryManager.getRecentHistory(currentRun.getLabel(), historySize);
        regressionResult = RegressionDetector.evaluate(currentRun, history);
        PerformanceHistoryManager.record(currentRun);
    }

    @Then("a performance report should be generated with the results")
    public void generate_report() {
        PerformanceReportGenerator.generate(currentRun, samples, selectedApiEndpoints());
    }

    @Then("a performance trend report should be generated with the historical results")
    public void generate_trend_report() {
        ensureRegressionEvaluated();
        int historySize = ConfigManager.getInstance().getPerformanceHistorySize();
        List<PerformanceRunRecord> history = PerformanceHistoryManager.getRecentHistory(currentRun.getLabel(), historySize);
        PerformanceTrendReportGenerator.generate(currentRun, history, regressionResult, selectedApiEndpoints());
    }

    private Map<String, String[]> selectedApiEndpoints() {
        Map<String, String[]> result = new LinkedHashMap<>();
        for (String api : selectedApis) {
            result.put(api, API_ENDPOINTS.getOrDefault(api, new String[]{"GET", "/" + api}));
        }
        return result;
    }
}
