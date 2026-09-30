package com.automation.utils;

import com.automation.config.ConfigManager;
import com.automation.model.PerformanceMetrics;
import com.automation.model.PerformanceRunRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the "Performance Trend Report" HTML dashboard (Chart.js): response time /
 * throughput / error-rate trend lines across historical runs sharing the same label,
 * plus the latest run's configuration, endpoints, and a run-history table (newest first).
 */
public final class PerformanceTrendReportGenerator {

    private static final Logger log = LoggerUtil.getLogger(PerformanceTrendReportGenerator.class);
    private static final Path REPORT_DIR = Paths.get("src", "test", "resources", "reports", "performance-trend");
    private static final ObjectMapper JSON = new ObjectMapper();

    private PerformanceTrendReportGenerator() {
    }

    public static void generate(PerformanceRunRecord latestRun, List<PerformanceRunRecord> history,
                                 RegressionDetector.Result latestRegressionResult,
                                 Map<String, String[]> apiEndpoints) {
        try {
            Files.createDirectories(REPORT_DIR);
            Path reportFile = REPORT_DIR.resolve("PerformanceTrendReport.html");
            try (Writer writer = Files.newBufferedWriter(reportFile)) {
                writer.write(buildHtml(latestRun, history, latestRegressionResult, apiEndpoints));
            }
            log.info("Performance trend report generated at {}", reportFile.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Unable to generate performance trend report: {}", e.getMessage());
        }
    }

    private static String buildHtml(PerformanceRunRecord latestRun, List<PerformanceRunRecord> history,
                                     RegressionDetector.Result latestRegressionResult,
                                     Map<String, String[]> apiEndpoints) {
        PerformanceMetrics latest = latestRun.getMetrics();
        ConfigManager config = ConfigManager.getInstance();

        List<String> timestamps = new ArrayList<>();
        List<Double> avgResponse = new ArrayList<>();
        List<Long> p95 = new ArrayList<>();
        List<Long> p99 = new ArrayList<>();
        List<Double> throughput = new ArrayList<>();
        List<Double> errorRate = new ArrayList<>();
        for (PerformanceRunRecord run : history) {
            timestamps.add(run.getTimestamp());
            avgResponse.add(run.getMetrics().getAvgResponseTimeMs());
            p95.add(run.getMetrics().getP95());
            p99.add(run.getMetrics().getP99());
            throughput.add(run.getMetrics().getThroughputPerSec());
            errorRate.add(run.getMetrics().getErrorRatePercent());
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"><title>Performance Trend Report</title>")
                .append("<script src=\"").append(ReportTheme.CHART_JS_CDN).append("\"></script>")
                .append("<style>").append(ReportTheme.css()).append("</style></head><body>");

        html.append("<h1>Performance Trend Report</h1>")
                .append("<div class=\"subtitle\">Showing last ").append(history.size())
                .append(history.size() == 1 ? " run" : " runs").append(" (oldest to newest) | Regression threshold: ")
                .append(config.getRegressionResponseTimeThresholdPercent()).append("%</div>");

        html.append("<div class=\"tiles\">")
                .append(ReportTheme.tile(String.valueOf(history.size()), "Total Runs", "info"))
                .append(ReportTheme.tile((long) latest.getAvgResponseTimeMs() + "ms", "Latest Avg Response", "warn"))
                .append(ReportTheme.tile(latest.getP95() + "ms", "Latest P95", "info"))
                .append(ReportTheme.tile(String.format("%.2f/s", latest.getThroughputPerSec()), "Latest Throughput", "ok"))
                .append(ReportTheme.tile(String.format("%.2f%%", latest.getErrorRatePercent()), "Latest Error Rate",
                        latest.getErrorRatePercent() > 0 ? "bad" : "ok"))
                .append(ReportTheme.tile(latestRegressionResult.isRegressionDetected() ? "1" : "0", "Regressions Detected",
                        latestRegressionResult.isRegressionDetected() ? "bad" : "ok"))
                .append("</div>");

        if (history.size() < 2) {
            html.append("<div class=\"section\"><div class=\"success-banner\" style=\"background:rgba(126,169,255,0.12);")
                    .append("border-color:rgba(126,169,255,0.35);color:#7ea9ff;\">")
                    .append("Only 1 run recorded so far - this point is today's baseline. Run the performance suite ")
                    .append("again to start seeing a trend line.</div></div>");
        }

        html.append("<div class=\"panel section\"><h2>Response Time Trend (ms)</h2><canvas id=\"responseTrendChart\"></canvas></div>");
        html.append("<div class=\"panel section\"><h2>Throughput Trend (req/sec)</h2><canvas id=\"throughputTrendChart\"></canvas></div>");
        html.append("<div class=\"panel section\"><h2>Error Rate Trend (%)</h2><canvas id=\"errorTrendChart\"></canvas></div>");

        html.append(testConfiguration(latestRun));
        html.append(endpointsUnderTest(apiEndpoints));
        html.append(runHistoryTable(history));

        html.append("<script>");
        html.append("const trendLabels=").append(toJson(timestamps)).append(";");
        String pointStyle = "pointRadius:6,pointHoverRadius:8,pointBackgroundColor:";
        html.append("new Chart(document.getElementById('responseTrendChart'),{type:'line',data:{labels:trendLabels,datasets:[")
                .append("{label:'Avg Response (ms)',data:").append(toJson(avgResponse)).append(",borderColor:'#4fd1ff',tension:.25,").append(pointStyle).append("'#4fd1ff'},")
                .append("{label:'P95 (ms)',data:").append(toJson(p95)).append(",borderColor:'#b6a6ff',tension:.25,").append(pointStyle).append("'#b6a6ff'},")
                .append("{label:'P99 (ms)',data:").append(toJson(p99)).append(",borderColor:'#ffb84f',tension:.25,").append(pointStyle).append("'#ffb84f'}")
                .append("]},options:trendOpts('ms')});");
        html.append("new Chart(document.getElementById('throughputTrendChart'),{type:'line',data:{labels:trendLabels,datasets:[")
                .append("{label:'Throughput (req/s)',data:").append(toJson(throughput)).append(",borderColor:'#3ddc84',tension:.25,").append(pointStyle).append("'#3ddc84'}")
                .append("]},options:trendOpts('req/s')});");
        html.append("new Chart(document.getElementById('errorTrendChart'),{type:'line',data:{labels:trendLabels,datasets:[")
                .append("{label:'Error Rate (%)',data:").append(toJson(errorRate)).append(",borderColor:'#ffb84f',tension:.25,").append(pointStyle).append("'#ffb84f'}")
                .append("]},options:trendOpts('%')});");
        html.append("function trendOpts(yLabel){return {responsive:true,plugins:{legend:{labels:{color:'#e8edff'}}},"
                + "scales:{x:{ticks:{color:'#9aa8d1'},grid:{color:'rgba(255,255,255,0.05)'}},"
                + "y:{title:{display:true,text:yLabel,color:'#9aa8d1'},ticks:{color:'#9aa8d1'},grid:{color:'rgba(255,255,255,0.05)'}}}};}");
        html.append("</script></body></html>");
        return html.toString();
    }

    private static String testConfiguration(PerformanceRunRecord run) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Environment", run.getEnvironment());
        rows.put("Concurrent Users", String.valueOf(run.getThreads()));
        rows.put("Ramp-up Period", run.getRampUpSeconds() + "s");
        rows.put("Duration", run.getDurationSeconds() + "s");
        rows.put("Total Samples", String.valueOf(run.getMetrics().getTotalRequests()));
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Test Configuration</h2><table><tr><th>Parameter</th><th>Value</th></tr>");
        rows.forEach((k, v) -> sb.append("<tr><td>").append(k).append("</td><td>").append(ReportTheme.htmlEscape(v)).append("</td></tr>"));
        sb.append("</table></div>");
        return sb.toString();
    }

    private static String endpointsUnderTest(Map<String, String[]> apiEndpoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Endpoints Under Test</h2><table><tr><th>#</th><th>Method</th><th>Full URL</th></tr>");
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

    private static String runHistoryTable(List<PerformanceRunRecord> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Run History (latest first)</h2><table>")
                .append("<tr><th>#</th><th>Timestamp</th><th>Env</th><th>Threads</th><th>Avg (ms)</th>")
                .append("<th>P95 (ms)</th><th>P99 (ms)</th><th>Throughput</th><th>Error%</th></tr>");
        int rowNum = 1;
        for (int i = history.size() - 1; i >= 0; i--) {
            PerformanceRunRecord run = history.get(i);
            PerformanceMetrics m = run.getMetrics();
            sb.append("<tr><td>").append(rowNum++).append("</td><td>").append(run.getTimestamp()).append("</td><td>")
                    .append(run.getEnvironment()).append("</td><td>").append(run.getThreads()).append("</td><td>")
                    .append((long) m.getAvgResponseTimeMs()).append("</td><td>").append(m.getP95()).append("</td><td>")
                    .append(m.getP99()).append("</td><td>").append(String.format("%.2f/s", m.getThroughputPerSec()))
                    .append("</td><td>").append(String.format("%.2f%%", m.getErrorRatePercent())).append("</td></tr>");
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
