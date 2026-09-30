package com.automation.utils;

import com.automation.config.ConfigManager;
import com.automation.model.PerformanceMetrics;
import com.automation.model.PerformanceRunRecord;
import com.automation.model.PerformanceSample;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the self-contained "Performance Test Report" HTML dashboard (Chart.js) for a
 * single load test run: summary tiles, response-time/throughput/thread/error time series,
 * a response-time distribution histogram, percentile bars, an aggregate table, a fixed-SLA
 * assertion summary, the run's configuration and the endpoints that were exercised.
 */
public final class PerformanceReportGenerator {

    private static final Logger log = LoggerUtil.getLogger(PerformanceReportGenerator.class);
    private static final Path REPORT_DIR = Paths.get("src", "test", "resources", "reports", "performance-report");
    private static final ObjectMapper JSON = new ObjectMapper();

    private PerformanceReportGenerator() {
    }

    public static void generate(PerformanceRunRecord run, List<PerformanceSample> samples,
                                 Map<String, String[]> apiEndpoints) {
        try {
            Files.createDirectories(REPORT_DIR);
            Path reportFile = REPORT_DIR.resolve("PerformanceReport.html");
            try (Writer writer = Files.newBufferedWriter(reportFile)) {
                writer.write(buildHtml(run, samples, apiEndpoints));
            }
            log.info("Performance report generated at {}", reportFile.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Unable to generate performance report: {}", e.getMessage());
        }
    }

    private static String buildHtml(PerformanceRunRecord run, List<PerformanceSample> samples,
                                     Map<String, String[]> apiEndpoints) {
        PerformanceMetrics m = run.getMetrics();
        int duration = run.getDurationSeconds();

        double[] avgPerSecond = new double[Math.max(duration, 1)];
        double[] throughputPerSecond = new double[Math.max(duration, 1)];
        double[] errorsPerSecond = new double[Math.max(duration, 1)];
        long[] sumPerSecond = new long[Math.max(duration, 1)];
        int[] countPerSecond = new int[Math.max(duration, 1)];

        int[] distribution = new int[7]; // 0-50,50-100,100-200,200-500,500-1000,1000-2000,2000+

        for (PerformanceSample s : samples) {
            int bucket = (int) Math.min(s.getElapsedMs() / 1000, duration - 1L);
            if (bucket >= 0 && bucket < countPerSecond.length) {
                sumPerSecond[bucket] += s.getLatencyMs();
                countPerSecond[bucket]++;
                if (!s.isSuccess()) {
                    errorsPerSecond[bucket]++;
                }
            }
            distribution[distributionBucket(s.getLatencyMs())]++;
        }
        for (int i = 0; i < duration; i++) {
            avgPerSecond[i] = countPerSecond[i] == 0 ? 0 : (double) sumPerSecond[i] / countPerSecond[i];
            throughputPerSecond[i] = countPerSecond[i];
        }

        int[] activeThreads = activeThreadsPerSecond(run.getThreads(), run.getRampUpSeconds(), duration);
        String[] elapsedLabels = new String[duration];
        for (int i = 0; i < duration; i++) {
            elapsedLabels[i] = i + "s";
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"><title>Performance Test Report</title>")
                .append("<script src=\"").append(ReportTheme.CHART_JS_CDN).append("\"></script>")
                .append("<style>").append(ReportTheme.css()).append("</style></head><body>");

        html.append("<h1>Performance Test Report</h1>")
                .append("<div class=\"subtitle\">Label: ").append(ReportTheme.htmlEscape(run.getLabel()))
                .append(" | Environment: ").append(ReportTheme.htmlEscape(run.getEnvironment()))
                .append(" | ").append(ReportTheme.htmlEscape(run.getTimestamp()))
                .append(" | Duration: ").append(duration).append("s</div>");

        html.append("<div class=\"tiles\">")
                .append(ReportTheme.tile(String.valueOf(m.getTotalRequests()), "Total Samples", "info"))
                .append(ReportTheme.tile(String.format("%.2f/s", m.getThroughputPerSec()), "Throughput", "ok"))
                .append(ReportTheme.tile((long) m.getAvgResponseTimeMs() + "ms", "Avg Response", "warn"))
                .append(ReportTheme.tile(m.getP90() + "ms", "90th Percentile", "info"))
                .append(ReportTheme.tile(m.getP95() + "ms", "95th Percentile", "info"))
                .append(ReportTheme.tile(String.format("%.2f%%", m.getErrorRatePercent()), "HTTP Error Rate",
                        m.getErrorRatePercent() > 0 ? "bad" : "ok"))
                .append(ReportTheme.tile(String.format("%.2f%%", m.getErrorRatePercent()), "Txn Status Fail Rate",
                        m.getErrorRatePercent() > 0 ? "bad" : "ok"))
                .append(ReportTheme.tile(m.getMinResponseTimeMs() + "ms", "Min Response", "ok"))
                .append(ReportTheme.tile(m.getMaxResponseTimeMs() + "ms", "Max Response", "bad"))
                .append("</div>");

        html.append("<div class=\"grid2\">")
                .append(chartPanel("Response Times Over Time", "responseTimeChart"))
                .append(chartPanel("Throughput (req/sec)", "throughputChart"))
                .append(chartPanel("Response Time Distribution", "distributionChart"))
                .append(chartPanel("Response Time Percentiles", "percentileChart"))
                .append(chartPanel("Active Threads Over Time", "threadsChart"))
                .append(chartPanel("Errors Over Time", "errorsChart"))
                .append("</div>");

        html.append(aggregateTable(run));
        html.append(errorSummary(m));
        html.append(assertionSummary(m));
        html.append(testConfiguration(run));
        html.append(endpointsUnderTest(apiEndpoints));

        html.append("<script>");
        html.append("const labels=").append(toJson(elapsedLabels)).append(";");
        html.append("new Chart(document.getElementById('responseTimeChart'),{type:'line',data:{labels,datasets:[{label:'Avg Response (ms)',data:")
                .append(toJson(avgPerSecond)).append(",borderColor:'#4fd1ff',tension:.3,pointRadius:0}]},options:baseOpts('Elapsed (s)','ms')});");
        html.append("new Chart(document.getElementById('throughputChart'),{type:'line',data:{labels,datasets:[{label:'Throughput (req/s)',data:")
                .append(toJson(throughputPerSecond)).append(",borderColor:'#3ddc84',tension:.3,pointRadius:0}]},options:baseOpts('Elapsed (s)','req/s')});");
        html.append("new Chart(document.getElementById('threadsChart'),{type:'line',data:{labels,datasets:[{label:'Active Threads',data:")
                .append(toJson(activeThreads)).append(",borderColor:'#ffb84f',stepped:true,pointRadius:0}]},options:baseOpts('Elapsed (s)','Threads')});");
        html.append("new Chart(document.getElementById('errorsChart'),{type:'line',data:{labels,datasets:[{label:'Errors',data:")
                .append(toJson(errorsPerSecond)).append(",borderColor:'#ff5c7a',tension:.3,pointRadius:0}]},options:baseOpts('Elapsed (s)','Errors')});");
        html.append("new Chart(document.getElementById('distributionChart'),{type:'bar',data:{labels:")
                .append(toJson(new String[]{"0-50ms", "50-100ms", "100-200ms", "200-500ms", "500-1000ms", "1000-2000ms", "2000ms+"}))
                .append(",datasets:[{label:'Samples',data:").append(toJson(distribution)).append(",backgroundColor:'#8e8ff0'}]},options:baseOpts('','Count')});");
        html.append("new Chart(document.getElementById('percentileChart'),{type:'bar',data:{labels:")
                .append(toJson(new String[]{"P50", "P75", "P90", "P95", "P99"}))
                .append(",datasets:[{label:'Response Time (ms)',data:")
                .append(toJson(new long[]{m.getP50(), m.getP75(), m.getP90(), m.getP95(), m.getP99()}))
                .append(",backgroundColor:['#4fd1ff','#3ddc84','#8e8ff0','#ffb84f','#ff5c7a']}]},options:baseOpts('','ms')});");
        html.append("function baseOpts(xLabel,yLabel){return {responsive:true,plugins:{legend:{labels:{color:'#e8edff'}}},"
                + "scales:{x:{title:{display:!!xLabel,text:xLabel,color:'#9aa8d1'},ticks:{color:'#9aa8d1'},grid:{color:'rgba(255,255,255,0.05)'}},"
                + "y:{title:{display:!!yLabel,text:yLabel,color:'#9aa8d1'},ticks:{color:'#9aa8d1'},grid:{color:'rgba(255,255,255,0.05)'}}}};}");
        html.append("</script></body></html>");
        return html.toString();
    }

    private static int distributionBucket(long latencyMs) {
        if (latencyMs < 50) return 0;
        if (latencyMs < 100) return 1;
        if (latencyMs < 200) return 2;
        if (latencyMs < 500) return 3;
        if (latencyMs < 1000) return 4;
        if (latencyMs < 2000) return 5;
        return 6;
    }

    private static int[] activeThreadsPerSecond(int threads, int rampUpSeconds, int durationSeconds) {
        int[] result = new int[Math.max(durationSeconds, 1)];
        if (threads <= 0 || durationSeconds <= 0) {
            return result;
        }
        double intervalSeconds = rampUpSeconds / (double) threads;
        for (int sec = 0; sec < durationSeconds; sec++) {
            int active = 0;
            for (int i = 0; i < threads; i++) {
                if (i * intervalSeconds <= sec) {
                    active++;
                }
            }
            result[sec] = active;
        }
        return result;
    }

    private static String chartPanel(String title, String canvasId) {
        return "<div class=\"panel\"><h2>" + title + "</h2><canvas id=\"" + canvasId + "\"></canvas></div>";
    }

    private static String aggregateTable(PerformanceRunRecord run) {
        PerformanceMetrics m = run.getMetrics();
        return "<div class=\"section\"><h2>Aggregate Report</h2><table>"
                + "<tr><th>Label</th><th>#Samples</th><th>Average</th><th>Median</th><th>90% Line</th>"
                + "<th>95% Line</th><th>99% Line</th><th>Min</th><th>Max</th><th>Error%</th><th>Throughput</th></tr>"
                + "<tr><td>" + ReportTheme.htmlEscape(run.getLabel()) + "</td><td>" + m.getTotalRequests() + "</td>"
                + "<td>" + (long) m.getAvgResponseTimeMs() + "ms</td><td>" + m.getP50() + "ms</td>"
                + "<td>" + m.getP90() + "ms</td><td>" + m.getP95() + "ms</td><td>" + m.getP99() + "ms</td>"
                + "<td>" + m.getMinResponseTimeMs() + "ms</td><td>" + m.getMaxResponseTimeMs() + "ms</td>"
                + "<td>" + String.format("%.2f%%", m.getErrorRatePercent()) + "</td>"
                + "<td>" + String.format("%.2f/s", m.getThroughputPerSec()) + "</td></tr>"
                + "</table></div>";
    }

    private static String errorSummary(PerformanceMetrics m) {
        if (m.getFailureCount() == 0) {
            return "<div class=\"section\"><h2>Error Summary</h2>"
                    + "<div class=\"success-banner\">&#10003; No errors detected. All samples passed HTTP and transaction status validation.</div></div>";
        }
        return "<div class=\"section\"><h2>Error Summary</h2>"
                + "<div class=\"success-banner\" style=\"background:rgba(255,92,122,0.12);border-color:rgba(255,92,122,0.35);color:#ff5c7a;\">"
                + m.getFailureCount() + " of " + m.getTotalRequests() + " samples failed (" + String.format("%.2f%%", m.getErrorRatePercent())
                + " error rate).</div></div>";
    }

    private static String assertionSummary(PerformanceMetrics m) {
        ConfigManager config = ConfigManager.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Assertion Summary</h2><table>")
                .append("<tr><th>Assertion</th><th>Threshold</th><th>Actual</th><th>Result</th></tr>");

        sb.append(assertionRow("Error Rate", "< " + config.getSlaMaxErrorRatePercent() + "%",
                String.format("%.2f%%", m.getErrorRatePercent()),
                m.getErrorRatePercent() < config.getSlaMaxErrorRatePercent()));
        sb.append(assertionRow("Throughput", ">= " + config.getSlaMinThroughputPerSec() + " req/s",
                String.format("%.2f req/s", m.getThroughputPerSec()),
                m.getThroughputPerSec() >= config.getSlaMinThroughputPerSec()));
        sb.append(assertionRow("Avg Response Time", "< " + (long) config.getSlaMaxAvgResponseTimeMs() + "ms",
                (long) m.getAvgResponseTimeMs() + "ms",
                m.getAvgResponseTimeMs() < config.getSlaMaxAvgResponseTimeMs()));
        sb.append(assertionRow("P90 Response Time", "< " + (long) config.getSlaMaxP90ResponseTimeMs() + "ms",
                m.getP90() + "ms",
                m.getP90() < config.getSlaMaxP90ResponseTimeMs()));
        sb.append(assertionRow("Total Samples", "> 0", String.valueOf(m.getTotalRequests()), m.getTotalRequests() > 0));
        boolean txnPass = m.getErrorRatePercent() < config.getSlaMaxErrorRatePercent();
        sb.append(assertionRow("Transaction Status", "PASS (error rate < " + config.getSlaMaxErrorRatePercent() + "%)",
                txnPass ? "PASS" : "FAIL", txnPass));

        sb.append("</table></div>");
        return sb.toString();
    }

    private static String assertionRow(String assertion, String threshold, String actual, boolean pass) {
        return "<tr><td>" + assertion + "</td><td>" + threshold + "</td><td>" + actual + "</td>"
                + "<td>" + (pass ? "<span class=\"pass\">&#10003; PASS</span>" : "<span class=\"fail\">&#10007; FAIL</span>") + "</td></tr>";
    }

    private static String testConfiguration(PerformanceRunRecord run) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Target Base URL", ConfigManager.getInstance().getBaseUrl());
        rows.put("Environment", run.getEnvironment());
        rows.put("Concurrent Users", String.valueOf(run.getThreads()));
        rows.put("Ramp-up Period", run.getRampUpSeconds() + "s");
        rows.put("Duration", run.getDurationSeconds() + "s");
        rows.put("Total Samples", String.valueOf(run.getMetrics().getTotalRequests()));
        return keyValueTable("Test Configuration", rows);
    }

    private static String endpointsUnderTest(Map<String, String[]> apiEndpoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Endpoints Under Test</h2><table>")
                .append("<tr><th>#</th><th>Method</th><th>Full URL</th></tr>");
        String baseUrl = ConfigManager.getInstance().getBaseUrl();
        int i = 1;
        for (Map.Entry<String, String[]> entry : apiEndpoints.entrySet()) {
            String[] methodAndPath = entry.getValue();
            sb.append("<tr><td>").append(i++).append("</td><td>").append(methodAndPath[0]).append("</td><td>")
                    .append(baseUrl).append(methodAndPath[1]).append("</td></tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }

    private static String keyValueTable(String title, Map<String, String> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>").append(title).append("</h2><table>")
                .append("<tr><th>Parameter</th><th>Value</th></tr>");
        for (Map.Entry<String, String> entry : rows.entrySet()) {
            sb.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(ReportTheme.htmlEscape(entry.getValue())).append("</td></tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException e) {
            return "[]";
        }
    }
}
