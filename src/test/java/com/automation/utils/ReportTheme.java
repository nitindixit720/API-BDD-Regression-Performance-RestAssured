package com.automation.utils;

/**
 * Shared dark-theme CSS and small HTML helpers reused by PerformanceReportGenerator,
 * PerformanceTrendReportGenerator and RegressionReportGenerator, so the three dashboards
 * look like one consistent report suite.
 */
public final class ReportTheme {

    public static final String CHART_JS_CDN = "https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js";

    private ReportTheme() {
    }

    public static String css() {
        return "body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:linear-gradient(135deg,#0f1c3f,#16255c 60%,#0f1c3f);"
                + "color:#e8edff;padding:28px 36px 60px;}"
                + "h1{margin:0 0 4px;font-size:28px;color:#4fd1ff;}"
                + "h2{font-size:18px;color:#4fd1ff;margin:0 0 14px;}"
                + ".subtitle{color:#9aa8d1;font-size:13px;margin-bottom:22px;}"
                + ".tiles{display:flex;flex-wrap:wrap;gap:14px;margin-bottom:26px;}"
                + ".tile{background:rgba(255,255,255,0.06);border:1px solid rgba(255,255,255,0.08);"
                + "border-radius:10px;padding:16px 20px;min-width:140px;flex:1;text-align:center;}"
                + ".tile .value{font-size:24px;font-weight:700;}"
                + ".tile .label{font-size:11px;letter-spacing:.05em;color:#9aa8d1;text-transform:uppercase;margin-top:4px;}"
                + ".ok{color:#3ddc84;} .warn{color:#ffb84f;} .bad{color:#ff5c7a;} .info{color:#7ea9ff;}"
                + ".grid2{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-bottom:22px;}"
                + ".panel{background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.08);"
                + "border-radius:10px;padding:18px;}"
                + "table{border-collapse:collapse;width:100%;font-size:13px;}"
                + "th{background:rgba(79,209,255,0.15);color:#4fd1ff;text-align:left;padding:9px 12px;}"
                + "td{padding:9px 12px;border-top:1px solid rgba(255,255,255,0.08);}"
                + "tr:hover td{background:rgba(255,255,255,0.04);}"
                + ".pass{color:#3ddc84;font-weight:600;} .fail{color:#ff5c7a;font-weight:600;} .skip{color:#ffd76b;font-weight:600;}"
                + ".badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:700;}"
                + ".badge.pass{background:rgba(61,220,132,0.15);} .badge.fail{background:rgba(255,92,122,0.15);}"
                + ".section{margin-bottom:26px;}"
                + ".success-banner{background:rgba(61,220,132,0.12);border:1px solid rgba(61,220,132,0.35);"
                + "color:#3ddc84;padding:12px 16px;border-radius:8px;font-size:13px;}"
                + "details{border:1px solid rgba(255,255,255,0.08);border-radius:8px;margin-bottom:8px;overflow:hidden;}"
                + "summary{cursor:pointer;padding:10px 14px;background:rgba(255,255,255,0.04);list-style:none;}"
                + "summary::-webkit-details-marker{display:none;}"
                + ".step-list{padding:10px 16px;font-size:12px;color:#c7d2f0;}"
                + ".step-list div{padding:3px 0;}"
                + ".feature-group summary{background:rgba(79,209,255,0.12);font-weight:600;}"
                + "canvas{max-height:280px;}";
    }

    public static String tile(String value, String label, String colorClass) {
        return "<div class=\"tile\"><div class=\"value " + colorClass + "\">" + value + "</div>"
                + "<div class=\"label\">" + label + "</div></div>";
    }

    public static String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
