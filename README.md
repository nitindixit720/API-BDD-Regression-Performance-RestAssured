# BDD API Performance Test Framework

A generic BDD-based API test automation framework built with **Cucumber + Rest Assured +
TestNG**. It supports both **Functional Regression Testing** and **Performance Load
Testing** from the same codebase, driven entirely by Gherkin feature files.

This is a clean-room, from-scratch scaffold intended as a reusable starting point - swap
the sample API (JSONPlaceholder) for your own, and extend the config/steps/reporting
layers as needed.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Environment Configuration](#environment-configuration)
- [How to Run Tests](#how-to-run-tests)
- [Test Scenarios](#test-scenarios)
- [Reporting](#reporting)
- [CI/CD Pipeline (Jenkins)](#cicd-pipeline-jenkins)
- [Framework Features](#framework-features)
- [Scope Notes](#scope-notes)
- [Troubleshooting](#troubleshooting)
- [Adapting This Framework to Your Own API](#adapting-this-framework-to-your-own-api)

## Architecture Overview

```
Test Execution Layer
  Cucumber (BDD/Gherkin)  |  TestNG (Test Runner)  |  Maven Surefire (Build Orchestrator)
                              v
Step Definitions
  UserManagementSteps (CRUD)  |  PerformanceSteps (Multi-threaded Load Testing)
                              v
API Client Layer
  ApiClient (Rest Assured) - GET | POST | PUT | DELETE
                              v
Support Layer
  ConfigManager (Env Config)  |  PayloadStore (Thread-safe)  |  ResponseValidator (Assertions)
                              v
Reporting Layer
  Extent Report (Cucumber Adapter)  |  Performance Report (Chart.js)
  Performance Trend Report (Chart.js)  |  Regression Report (Chart.js, from cucumber.json)
```

## Tech Stack

| Component        | Technology                       | Version |
|-------------------|-----------------------------------|---------|
| Language           | Java                              | 17      |
| Build Tool         | Apache Maven                      | 3.8+    |
| BDD Framework      | Cucumber                          | 7.15.0  |
| HTTP Client        | Rest Assured                      | 5.4.0   |
| Test Runner        | TestNG                            | 7.9.0   |
| Reporting          | Extent Reports (Cucumber Adapter) | 1.14.0  |
| Logging            | SLF4J + Logback                   | 2.0.9 / 1.4.14 |
| JSON Processing    | Jackson Databind                  | 2.16.1  |
| CI/CD              | Jenkins (Declarative Pipeline)    | -       |

> Jackson Databind is included as an extension point for POJO-based request/response
> mapping; current step definitions use plain `Map`s, which Rest Assured serializes
> directly - swap in typed models as the framework grows.
>
> The Extent Reports Cucumber adapter's Maven coordinate is
> `tech.grasshopper:extentreports-cucumber7-adapter:1.14.0` (its Java package is still
> `com.aventstack.extentreports.cucumber.adapter`, referenced in `TestRunner`'s plugin list).

## Prerequisites

| Software          | Minimum Version | Download Link |
|--------------------|------------------|----------------|
| Java JDK           | 17               | https://www.oracle.com/java/technologies/downloads/ |
| Apache Maven       | 3.8.x            | https://maven.apache.org/download.cgi |
| Git                | 2.x              | https://git-scm.com/downloads |
| IDE (recommended)  | IntelliJ IDEA    | https://www.jetbrains.com/idea/download/ |

Verify installation:
```bash
java -version   # Expected: java version "17.x.x"
mvn -version    # Expected: Apache Maven 3.8.x or higher
git --version   # Expected: git version 2.x.x
```

No VPN or special network access is required - the default DEV environment targets the
public JSONPlaceholder API.

## Project Structure

```
BDD-API-Performance-Test/
├── pom.xml                          # Maven build configuration & dependencies
├── testng.xml                       # TestNG suite configuration
├── Jenkinsfile                      # CI/CD pipeline definition
├── logs/
│   └── execution.log                # Runtime execution logs (generated)
├── src/
│   ├── main/
│   │   ├── java/com/automation/
│   │   │   ├── client/
│   │   │   │   └── ApiClient.java             # REST client (GET/POST/PUT/DELETE)
│   │   │   ├── config/
│   │   │   │   ├── ConfigManager.java         # Environment-specific config loader
│   │   │   │   ├── PerformanceConfigManager.java # Performance test settings
│   │   │   │   └── PropertiesLoader.java      # Properties file utility
│   │   │   ├── constants/
│   │   │   │   └── EndPoints.java             # API endpoint constants
│   │   │   ├── model/
│   │   │   │   ├── PerformanceMetrics.java    # Pooled run metrics (percentiles, throughput, error rate)
│   │   │   │   ├── PerformanceSample.java     # One request's result (elapsed offset, latency, success)
│   │   │   │   └── PerformanceRunRecord.java  # A run's config + metrics, persisted to history CSV
│   │   │   └── utils/
│   │   │       ├── LoggerUtil.java            # Centralized logging utility
│   │   │       └── PayloadStore.java          # Thread-safe request/response store
│   │   └── resources/
│   │       ├── config/
│   │       │   ├── config.properties          # Shared defaults
│   │       │   ├── config-DEV.properties       # DEV environment config
│   │       │   └── config-QA.properties        # QA environment config
│   │       └── logback.xml                     # Logging configuration
│   └── test/
│       ├── java/com/automation/
│       │   ├── base/
│       │   │   └── BaseTest.java               # Shared test base
│       │   ├── hooks/
│       │   │   └── Hooks.java                  # Cucumber lifecycle hooks
│       │   ├── runners/
│       │   │   └── TestRunner.java             # TestNG + Cucumber runner
│       │   ├── stepdefinitions/
│       │   │   ├── CommonSteps.java            # Shared background step
│       │   │   ├── UserManagementSteps.java    # Functional/regression step definitions
│       │   │   └── PerformanceSteps.java       # Performance test step definitions
│       │   ├── utils/
│       │   │   ├── PayloadBuilder.java         # Request payload builder
│       │   │   ├── PerformanceHistoryManager.java # Historical run storage (CSV)
│       │   │   ├── RegressionDetector.java     # Rolling-average performance regression detector
│       │   │   ├── ReportTheme.java            # Shared dark-theme CSS/HTML helpers for all 3 dashboards
│       │   │   ├── PerformanceReportGenerator.java      # Chart.js performance dashboard (per run)
│       │   │   ├── PerformanceTrendReportGenerator.java # Chart.js trend dashboard (across runs)
│       │   │   ├── RegressionReportGenerator.java       # Chart.js functional-results dashboard (from cucumber.json)
│       │   │   ├── RegressionReportSuiteListener.java   # TestNG listener that triggers the report above
│       │   │   ├── RetryAnalyzer.java          # TestNG retry mechanism
│       │   │   ├── RetryTransformer.java       # TestNG retry auto-registration
│       │   │   └── TestDataProvider.java       # Test data (CSV) loader
│       │   └── validations/
│       │       └── ResponseValidator.java      # Response assertion library
│       └── resources/
│           ├── features/
│           │   ├── functional.feature          # Functional/regression BDD scenarios
│           │   └── performance.feature         # Performance BDD scenarios
│           ├── testdata/
│           │   └── posts.csv                   # Sample record IDs for performance runs
│           ├── reports/                        # Generated test reports
│           └── extent.properties               # Extent Reports configuration
```

## Environment Configuration

The framework uses environment-specific property files. The environment is selected via
the `-Denv` system property (defaults to `DEV` if omitted).

| Environment    | -Denv Value | Auth Required | Base URL |
|-----------------|-------------|----------------|----------|
| DEV (default)   | `DEV`       | No             | https://jsonplaceholder.typicode.com |
| QA              | `QA`        | Yes (demo pattern) | https://jsonplaceholder.typicode.com |

> QA demonstrates the bearer-token config pattern for a secured environment. The demo API
> does not actually validate the token - replace `base.url` and `bearer.token` in
> `config-QA.properties`, or export `API_BEARER_TOKEN`, when pointing this at a real
> secured API.

## How to Run Tests

### Regression Tests

```bash
# Full regression suite (DEV environment - default)
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@Regression"

# Smoke tests only
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@Smoke"

# BAT (Build Acceptance Tests) only
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@BAT"

# Positive scenarios only
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@Positive"

# Negative scenarios only
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@Negative"

# Run against QA environment
mvn clean test -Denv=QA "-Dcucumber.filter.tags=@Regression"
```

### Performance Tests

```bash
# Default performance test (10 threads, 30s ramp-up, 120s duration, all APIs)
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@PerformanceTest"

# Custom load configuration
mvn clean test -Denv=DEV ^
    -Dthreads=25 ^
    -DrampUp=30 ^
    -Dduration=1800 ^
    "-Dcucumber.filter.tags=@PerformanceTest"

# Select specific APIs to test
mvn clean test -Denv=DEV ^
    -Dthreads=15 ^
    -DrampUp=10 ^
    -Dduration=120 ^
    -Dapis=Get-Posts,Get-Post-By-Id ^
    "-Dcucumber.filter.tags=@PerformanceTest"

# Lightweight smoke-level load test (fixed 2 threads / 5s ramp-up / 15s duration)
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@PerformanceSmoke"
```

(Use `\` instead of `^` for line continuation on macOS/Linux shells.)

### Performance Test Parameters

| Parameter | System Property | Default | Description |
|-----------|------------------|---------|--------------|
| Threads    | `-Dthreads`  | 10  | Number of concurrent virtual users |
| Ramp-Up    | `-DrampUp`   | 30  | Time (seconds) to reach full thread count |
| Duration   | `-Dduration` | 120 | Total test duration in seconds |
| APIs       | `-Dapis`     | ALL | Comma-separated API names to include |

### Available API Selections for Performance Tests

| API Name         | HTTP Method | Endpoint Path         |
|-------------------|-------------|------------------------|
| Get-Posts          | GET         | `/posts`               |
| Get-Post-By-Id     | GET         | `/posts/{id}`          |
| Get-Comments       | GET         | `/posts/{id}/comments` |
| Create-Post        | POST        | `/posts`               |

## Test Scenarios

`functional.feature` (6 scenarios): list all posts, get post by ID, create post, update
post, delete post, and a negative 404 lookup - tagged `@Regression`, `@Smoke`, `@BAT`,
`@Positive`, `@Negative` to mirror common CI trigger patterns.

`performance.feature` (2 scenarios): a fully configurable concurrent load test driven by
system properties (which also generates the Performance Trend Report), and a fixed
lightweight smoke-level load test that runs out of the box with no extra flags.

## Reporting

Four report types are generated, all self-contained HTML (Chart.js loaded from CDN for the
three dashboards below - an internet connection is needed to *view* the charts, not to run
the tests).

1. **Extent Report (Cucumber Adapter)**
   - Location: `src/test/resources/reports/extent-report/ExtentReport.html`
   - Content: step-level execution details, pass/fail status per scenario.
   - Generated automatically by the `ExtentCucumberAdapter` plugin as scenarios run.

2. **Performance Report (Chart.js)**
   - Location: `src/test/resources/reports/performance-report/PerformanceReport.html`
   - Generated by: `PerformanceReportGenerator`, triggered by the
     `a performance report should be generated with the results` step.
   - Content: summary tiles (samples, throughput, avg/P90/P95, error rate, min/max), six
     charts (response time / throughput / active threads / errors over time, response-time
     distribution histogram, P50-P99 percentile bars), an aggregate table, an error summary,
     a **fixed-SLA assertion summary** (error rate/throughput/avg/P90 vs the
     `performance.sla.*` thresholds in `config.properties` - informational, does not fail
     the scenario), the run's configuration, and the endpoints exercised.

3. **Performance Trend Report (Chart.js)**
   - Location: `src/test/resources/reports/performance-trend/PerformanceTrendReport.html`
   - Generated by: `PerformanceTrendReportGenerator`, triggered by the
     `a performance trend report should be generated with the historical results` step
     (main performance scenario only, not the smoke variant).
   - Content: summary tiles including **Regressions Detected**, three trend line charts
     (avg/P95/P99 response time, throughput, error rate) across up to
     `performance.history.size` historical runs sharing the same label, the latest run's
     configuration/endpoints, and a run-history table (newest first). With fewer than 2
     historical runs, the charts show a single clearly-marked point plus a banner
     explaining it's the baseline (a line chart can't draw a line from one point).
   - Also emits `PerformanceTrend.json` in the same folder - the same data (label,
     regression thresholds/result, latest run, and full history) as structured JSON, for
     any external tooling/dashboard that wants to consume trend data without parsing HTML.

4. **Regression Report (Chart.js, functional results)**
   - Location: `src/test/resources/reports/regression-report/RegressionReport.html`
   - Generated by: `RegressionReportGenerator`, parsing Cucumber's own `json:` output
     (`target/cucumber-reports/cucumber.json`) via `RegressionReportSuiteListener`
     (a TestNG `ISuiteListener`, registered in `testng.xml`) - it runs once, after the
     *entire* `mvn test` invocation finishes, so it always reflects every scenario that ran
     (functional and/or performance), not just one feature file.
   - Content: a pass-rate gauge, summary tiles, a pass/fail/skip donut chart, a
     scenarios-per-feature bar chart, a feature summary table, and an expandable
     scenario-by-scenario breakdown showing every step's status - the step-level detail is
     what drives root-cause analysis for a failure (see Scope Notes below for what this
     intentionally leaves out).

5. **Performance History (CSV)** and **Console / File Logs** remain as raw inputs to the
   above: `target/performance-reports/history/performance_history.csv` (regression baseline)
   and `logs/execution.log` (pattern: `yyyy-MM-dd HH:mm:ss [thread] LEVEL logger - message`).

## CI/CD Pipeline (Jenkins)

The included `Jenkinsfile` provides a parameterized declarative pipeline:

| Parameter              | Options                                          | Description |
|-------------------------|---------------------------------------------------|--------------|
| TEST_TYPE               | Regression, Performance                            | Select test type |
| FUNCTIONAL_TEST_SCOPE   | Regression, Smoke, BAT                             | Functional test scope |
| ENVIRONMENT             | DEV, QA                                            | Target environment |
| THREADS                 | 10, 15, 20, 25, 30, 40, 50                          | Concurrent users (Performance) |
| RAMP_UP                 | 10, 30, 60, 100, 200                                | Ramp-up seconds (Performance) |
| DURATION                | 120, 300, 600, 1800, 3600                           | Duration seconds (Performance) |
| API_SELECTION           | comma-separated API names, or ALL                   | APIs to load test |

## Framework Features

- **BDD Approach**: Gherkin feature files for human-readable scenarios.
- **Multi-Environment Support**: externalized config per environment via `-Denv`.
- **Bearer Token Auth Pattern**: optional per-environment auth, env-var override for CI/CD secrets.
- **Thread-Safe Design**: `ThreadLocal` payload storage for safe parallel execution.
- **Auto-Retry Mechanism**: failed scenarios retry up to 2 times via `RetryAnalyzer`.
- **Performance Regression Detection**: two independent mechanisms - a rolling-average
  comparison against the last N historical runs (`regression.threshold.*` in
  `config.properties`, surfaced in the Trend Report), and a fixed-SLA check against a
  single run (`performance.sla.*`, surfaced in the Performance Report's Assertion Summary).
- **Four Self-Contained HTML Reports**: Extent, Performance, Performance Trend and
  Regression (Chart.js) - no external report server required.
- **CI/CD Ready**: parameterized Jenkins pipeline included.

## Scope Notes

This is a deliberately generic scaffold. A few things were simplified or left as extension
points rather than fully built out:

- **Both regression checks are informational, not hard test failures** - they log a
  warning / render as FAIL in the report tables, but do not fail the scenario. Harden
  `PerformanceSteps` if you want a build to go red/unstable on regression.
- **No test-management-tool integration** (e.g. qTest/Xray) is included, since it requires
  real credentials/instance access. Add an integration layer in `utils/` if needed.
- **The Regression Report shows step-level pass/fail, not raw request/response payloads.**
  Cucumber's JSON output reliably exposes step name/status/duration across versions;
  embedding request/response bodies would mean parsing Cucumber's attachment format, which
  is more version-fragile for comparatively little extra value over per-step failure
  detail. Attach payloads via `Scenario.attach(...)` in `Hooks` if you want to extend this.
- **Chart.js is loaded from a CDN** (`ReportTheme.CHART_JS_CDN`) rather than vendored, to
  keep the repo dependency-light - the reports need internet access to render charts when
  opened, even though the *tests* that generate them don't (aside from hitting the API).

## Troubleshooting

| Issue                     | Resolution |
|----------------------------|------------|
| Connection timeout          | Check network access to the target base URL; increase `connection.timeout`/`socket.timeout` |
| 404 on functional scenario  | Expected for the negative test case; verify the ID used exists for other scenarios |
| Maven build failure         | Run `mvn clean install -DskipTests` to resolve dependency issues |
| Config not found            | Ensure `-Denv` value matches the suffix in `config-<ENV>.properties` |

### Useful Maven Commands

```bash
# Download dependencies only
mvn dependency:resolve

# Compile without running tests
mvn clean compile -DskipTests

# Run with verbose output
mvn clean test -Denv=DEV "-Dcucumber.filter.tags=@Smoke" -X
```

## Adapting This Framework to Your Own API

1. Update `base.url` in `src/main/resources/config/config-*.properties`.
2. Replace the paths in `EndPoints.java` with your API's endpoints.
3. Rewrite `functional.feature` and `UserManagementSteps.java` for your resource's CRUD
   contract (fields, status codes, validation rules).
4. Update the `Get-*`/`Create-*` API names in `PerformanceConfigManager` and
   `PerformanceSteps.invokeApi(...)` to match your endpoints.
5. If your API requires real OAuth2/API-key auth, extend `ConfigManager`/`ApiClient` to
   fetch and cache a token instead of using a static bearer token.

## License

MIT License - see [LICENSE](LICENSE).
