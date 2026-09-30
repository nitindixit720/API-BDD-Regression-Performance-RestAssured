package com.automation.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the Cucumber-native JSON results file (produced by the "json:" plugin configured
 * on TestRunner) and renders a "Regression Test Report" HTML dashboard (Chart.js): a
 * pass-rate gauge, summary tiles, pass/fail/skip breakdown charts, a feature summary table,
 * and expandable per-scenario step-level detail for failure root-causing.
 */
public final class RegressionReportGenerator {

    private static final Logger log = LoggerUtil.getLogger(RegressionReportGenerator.class);
    private static final Path CUCUMBER_JSON = Paths.get("target", "cucumber-reports", "cucumber.json");
    private static final Path REPORT_DIR = Paths.get("src", "test", "resources", "reports", "regression-report");
    private static final ObjectMapper JSON = new ObjectMapper();

    private RegressionReportGenerator() {
    }

    public static void generate() {
        if (!Files.exists(CUCUMBER_JSON)) {
            log.warn("Cucumber JSON results not found at {} - skipping regression report", CUCUMBER_JSON);
            return;
        }
        try {
            JsonNode features = JSON.readTree(CUCUMBER_JSON.toFile());
            List<ScenarioResult> scenarios = parseScenarios(features);
            String html = buildHtml(scenarios);

            Files.createDirectories(REPORT_DIR);
            Path reportFile = REPORT_DIR.resolve("RegressionReport.html");
            try (Writer writer = Files.newBufferedWriter(reportFile)) {
                writer.write(html);
            }
            log.info("Regression report generated at {}", reportFile.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Unable to generate regression report: {}", e.getMessage());
        }
    }

    private static List<ScenarioResult> parseScenarios(JsonNode features) {
        List<ScenarioResult> scenarios = new ArrayList<>();
        for (JsonNode feature : features) {
            String featureName = feature.path("name").asText("Unnamed Feature");
            for (JsonNode element : feature.path("elements")) {
                if (!"scenario".equals(element.path("type").asText())) {
                    continue;
                }
                ScenarioResult scenario = new ScenarioResult();
                scenario.featureName = featureName;
                scenario.name = element.path("name").asText("Unnamed Scenario");
                scenario.tags = new ArrayList<>();
                for (JsonNode tag : element.path("tags")) {
                    scenario.tags.add(tag.path("name").asText());
                }

                long totalDurationNs = 0;
                boolean anyFailed = false;
                boolean anySkipped = false;
                for (JsonNode step : element.path("steps")) {
                    String status = step.path("result").path("status").asText("unknown");
                    long durationNs = step.path("result").path("duration").asLong(0);
                    totalDurationNs += durationNs;
                    if ("failed".equals(status)) {
                        anyFailed = true;
                    } else if (!"passed".equals(status)) {
                        anySkipped = true;
                    }
                    StepResult stepResult = new StepResult();
                    stepResult.keyword = step.path("keyword").asText("");
                    stepResult.name = step.path("name").asText("");
                    stepResult.status = status;
                    scenario.steps.add(stepResult);
                }
                scenario.status = anyFailed ? "failed" : (anySkipped ? "skipped" : "passed");
                scenario.durationMs = totalDurationNs / 1_000_000L;
                scenarios.add(scenario);
            }
        }
        return scenarios;
    }

    private static String buildHtml(List<ScenarioResult> scenarios) {
        int total = scenarios.size();
        long passed = scenarios.stream().filter(s -> "passed".equals(s.status)).count();
        long failed = scenarios.stream().filter(s -> "failed".equals(s.status)).count();
        long skipped = total - passed - failed;
        double passRate = total == 0 ? 0 : (passed * 100.0) / total;
        long totalDurationMs = scenarios.stream().mapToLong(s -> s.durationMs).sum();
        long featureCount = scenarios.stream().map(s -> s.featureName).distinct().count();

        Map<String, long[]> perFeature = new LinkedHashMap<>(); // [total, passed, failed, skipped]
        for (ScenarioResult s : scenarios) {
            long[] counts = perFeature.computeIfAbsent(s.featureName, k -> new long[4]);
            counts[0]++;
            if ("passed".equals(s.status)) counts[1]++;
            else if ("failed".equals(s.status)) counts[2]++;
            else counts[3]++;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"><title>Regression Test Report</title>")
                .append("<script src=\"").append(ReportTheme.CHART_JS_CDN).append("\"></script>")
                .append("<style>").append(ReportTheme.css())
                .append(".gauge-wrap{position:relative;width:120px;height:120px;float:right;margin-top:-70px;}")
                .append(".gauge-text{position:absolute;top:0;left:0;width:100%;height:100%;display:flex;flex-direction:column;"
                        + "align-items:center;justify-content:center;font-weight:700;}")
                .append(".gauge-text .pct{font-size:20px;color:#3ddc84;} .gauge-text .lbl{font-size:9px;color:#9aa8d1;}")
                .append("</style></head><body>");

        html.append("<div class=\"gauge-wrap\"><canvas id=\"gaugeChart\"></canvas>")
                .append("<div class=\"gauge-text\"><div class=\"pct\">").append(String.format("%.0f%%", passRate))
                .append("</div><div class=\"lbl\">PASS RATE</div></div></div>");

        html.append("<h1>&#128220; Regression Test Report</h1>")
                .append("<div class=\"subtitle\">Generated: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append(" | Total Duration: ").append(String.format("%.1fs", totalDurationMs / 1000.0)).append("</div>");

        html.append("<div class=\"tiles\">")
                .append(ReportTheme.tile(String.valueOf(total), "Total Scenarios", "info"))
                .append(ReportTheme.tile(String.valueOf(passed), "Passed", "ok"))
                .append(ReportTheme.tile(String.valueOf(failed), "Failed", failed > 0 ? "bad" : "ok"))
                .append(ReportTheme.tile(String.valueOf(skipped), "Skipped", skipped > 0 ? "warn" : "ok"))
                .append(ReportTheme.tile(String.format("%.1f%%", passRate), "Pass Rate", "warn"))
                .append(ReportTheme.tile(String.valueOf(featureCount), "Features", "info"))
                .append(ReportTheme.tile(String.format("%.1fs", totalDurationMs / 1000.0), "Total Duration", "warn"))
                .append(ReportTheme.tile(String.valueOf(failed), "Failures", failed > 0 ? "bad" : "ok"))
                .append("</div>");

        html.append("<div class=\"grid2\">")
                .append("<div class=\"panel\"><h2>Test Results Overview</h2><canvas id=\"resultsDonut\"></canvas></div>")
                .append("<div class=\"panel\"><h2>Scenarios per Feature</h2><canvas id=\"featureBar\"></canvas></div>")
                .append("</div>");

        html.append(featureSummaryTable(perFeature));
        html.append(scenarioDetails(scenarios));

        html.append("<script>");
        html.append("new Chart(document.getElementById('gaugeChart'),{type:'doughnut',data:{datasets:[{data:[")
                .append(passRate).append(",").append(100 - passRate)
                .append("],backgroundColor:['#3ddc84','rgba(255,255,255,0.08)'],borderWidth:0}]},")
                .append("options:{cutout:'78%',plugins:{legend:{display:false},tooltip:{enabled:false}}}});");

        html.append("new Chart(document.getElementById('resultsDonut'),{type:'doughnut',data:{labels:['Passed','Failed','Skipped'],")
                .append("datasets:[{data:[").append(passed).append(",").append(failed).append(",").append(skipped)
                .append("],backgroundColor:['#3ddc84','#ff5c7a','#ffd76b']}]},")
                .append("options:{plugins:{legend:{labels:{color:'#e8edff'}}}}});");

        List<String> featureNames = new ArrayList<>(perFeature.keySet());
        StringBuilder passedArr = new StringBuilder("[");
        StringBuilder failedArr = new StringBuilder("[");
        for (String name : featureNames) {
            long[] counts = perFeature.get(name);
            passedArr.append(counts[1]).append(",");
            failedArr.append(counts[2]).append(",");
        }
        passedArr.append("]");
        failedArr.append("]");

        html.append("new Chart(document.getElementById('featureBar'),{type:'bar',data:{labels:")
                .append(toJsonArray(featureNames)).append(",datasets:[")
                .append("{label:'Passed',data:").append(passedArr).append(",backgroundColor:'#3ddc84'},")
                .append("{label:'Failed',data:").append(failedArr).append(",backgroundColor:'#ff5c7a'}")
                .append("]},options:{plugins:{legend:{labels:{color:'#e8edff'}}},scales:{x:{stacked:true,ticks:{color:'#9aa8d1'}},"
                        + "y:{stacked:true,ticks:{color:'#9aa8d1'},grid:{color:'rgba(255,255,255,0.05)'}}}}});");
        html.append("</script></body></html>");
        return html.toString();
    }

    private static String featureSummaryTable(Map<String, long[]> perFeature) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Feature Summary</h2><table>")
                .append("<tr><th>Feature</th><th>Total</th><th>Passed</th><th>Failed</th><th>Skipped</th><th>Pass Rate</th><th>Status</th></tr>");
        perFeature.forEach((name, counts) -> {
            double passRate = counts[0] == 0 ? 0 : (counts[1] * 100.0) / counts[0];
            boolean pass = counts[2] == 0;
            sb.append("<tr><td>").append(ReportTheme.htmlEscape(name)).append("</td><td>").append(counts[0])
                    .append("</td><td>").append(counts[1]).append("</td><td>").append(counts[2])
                    .append("</td><td>").append(counts[3]).append("</td><td>").append(String.format("%.1f%%", passRate))
                    .append("</td><td><span class=\"badge ").append(pass ? "pass" : "fail").append("\">")
                    .append(pass ? "PASS" : "FAIL").append("</span></td></tr>");
        });
        sb.append("</table></div>");
        return sb.toString();
    }

    private static String scenarioDetails(List<ScenarioResult> scenarios) {
        Map<String, List<ScenarioResult>> byFeature = new LinkedHashMap<>();
        for (ScenarioResult s : scenarios) {
            byFeature.computeIfAbsent(s.featureName, k -> new ArrayList<>()).add(s);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>Scenario Details</h2>");
        byFeature.forEach((featureName, list) -> {
            sb.append("<details class=\"feature-group\" open><summary>&#128220; ").append(ReportTheme.htmlEscape(featureName)).append("</summary>");
            for (ScenarioResult s : list) {
                String badgeClass = "passed".equals(s.status) ? "pass" : ("failed".equals(s.status) ? "fail" : "skip");
                String badgeText = s.status.toUpperCase();
                sb.append("<details><summary><span class=\"").append(badgeClass).append("\">&#10003; ").append(badgeText)
                        .append("</span> &nbsp; ").append(ReportTheme.htmlEscape(s.name))
                        .append(" &nbsp; <span style=\"color:#9aa8d1;\">").append(s.durationMs).append("ms")
                        .append(" | ").append(String.join(" ", s.tags)).append("</span></summary>");
                sb.append("<div class=\"step-list\">");
                for (StepResult step : s.steps) {
                    String stepClass = "passed".equals(step.status) ? "pass" : ("failed".equals(step.status) ? "fail" : "skip");
                    sb.append("<div><span class=\"").append(stepClass).append("\">&#8226;</span> ")
                            .append(ReportTheme.htmlEscape(step.keyword)).append(ReportTheme.htmlEscape(step.name))
                            .append(" <em style=\"color:#6b7aa8;\">[").append(step.status).append("]</em></div>");
                }
                sb.append("</div></details>");
            }
            sb.append("</details>");
        });
        sb.append("</div>");
        return sb.toString();
    }

    private static String toJsonArray(List<String> values) {
        try {
            return JSON.writeValueAsString(values);
        } catch (IOException e) {
            return "[]";
        }
    }

    private static final class ScenarioResult {
        String featureName;
        String name;
        String status;
        long durationMs;
        List<String> tags = new ArrayList<>();
        List<StepResult> steps = new ArrayList<>();
    }

    private static final class StepResult {
        String keyword;
        String name;
        String status;
    }
}
