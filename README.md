# RR QA Automation Assignment
### TMDB Discover — UI & API Test Automation Suite

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [How to Run](#how-to-run)
- [Testing Strategy](#testing-strategy)
- [Test Cases](#test-cases)
- [Test Design Techniques](#test-design-techniques)
- [Design Patterns](#design-patterns)
- [Browser API (CDP) Usage](#browser-api-cdp-usage)
- [Logging](#logging)
- [Reporting](#reporting)
- [CI Integration Approach](#ci-integration-approach)
- [Defects Found](#defects-found)

---

## Tech Stack

| Tool / Library | Version | Purpose |
|---|---|---|
| Java | 21 | Programming language |
| Selenium WebDriver | 4.28.1 | Browser automation |
| TestNG | 7.10.2 | Test framework and runner |
| ExtentReports | 5.1.1 | HTML test reporting |
| WebDriverManager | 5.9.2 | Automatic ChromeDriver management |
| Apache Commons IO | 2.15.1 | File I/O utilities |
| Maven | 3.x | Build and dependency management |
| Java Util Logging (JUL) | Built-in | Console and file logging |

---

## Project Structure

```
rr-qa-automation-assignment/
├── src/
│   ├── main/java/com/rapyuta/qa/
│   │   ├── pages/
│   │   │   └── HomePage.java          # Page Object Model for TMDB Discover
│   │   └── utils/
│   │       ├── DriverFactory.java     # ThreadLocal WebDriver management
│   │       └── LogFileUtil.java       # Network log file writer
│   └── test/java/com/rapyuta/qa/
│       └── tests/
│           ├── BaseTest.java          # ExtentReports setup, teardown, screenshots
│           ├── FilterTests.java       # TC-F1 to TC-F8 filter test cases
│           └── PaginationTests.java   # TC-P1, TC-P2 pagination test cases
├── testng.xml                         # Test suite configuration
├── pom.xml                            # Maven dependencies
├── README.md
└── TestPlan.md
```

---

## Setup Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- Google Chrome (latest)
- Git

### Clone and Install

```bash
git clone https://github.com/anjali252/rr-qa-automation-assignment.git
cd rr-qa-automation-assignment
mvn clean install -DskipTests
```

WebDriverManager automatically downloads and configures the correct ChromeDriver — no manual setup needed.

---

## How to Run

| Command | Description |
|---|---|
| `mvn clean test` | Run full suite in headed Chrome |
| `mvn clean test -Dheadless=true` | Run in headless Chrome (for CI) |

### Report Location
After the run, open the HTML report at:
```
target/extent-report.html
```

Network logs are written to:
```
reports/networkLogs/
```

---

## Testing Strategy

The suite is designed around three pillars:

**1. Functional UI Testing**
Each filter option (Type, Genre, Year, Rating, Category) is tested with valid inputs to verify the UI responds correctly and returns results. Category switching (Popular, Trend, Newest, Top Rated) is validated for non-empty, valid results. Search term input is tested to confirm the app returns contextually relevant results.

**2. Negative / Edge Case Testing**
Known broken behaviour is deliberately tested to document defects:
- Direct URL access to `/popular` and `/top` (BUG-01)
- Last page pagination on Popular (57,034 pages) (BUG-02)
- Year range filter returning out-of-range results (BUG-03)
- Genre filter returning mixed-genre results (BUG-04)

**3. Browser API Call Assertions (CDP)**
Selenium 4's Chrome DevTools Protocol (CDP) is used to intercept and assert browser-level network calls triggered by UI interactions. Performance logs capture API requests made to the TMDB backend when filters are applied and pagination is navigated.

---

## Test Cases

### Filter Tests (`FilterTests.java`)

| TC ID | Title | Type | Expected Result | Bug |
|---|---|---|---|---|
| TC-F1 | Popular category loads | Positive | 20 results on page load | — |
| TC-F2 | Switch categories | Positive | Each category returns valid, non-empty results | — |
| TC-F3 | Filter by Type (Movie / TV) | Positive | Results are non-empty for Movie and TV selections | — |
| TC-F4 | Filter by Year range | Negative | Results within selected range — BUG-03 causes failure | BUG-03 |
| TC-F5 | Search by keyword | Positive | Results are relevant to the entered search term | — |
| TC-F6 | Filter by Rating | Positive | Stricter rating threshold returns fewer results | — |
| TC-F7 | Filter by Genre | Negative | Results match selected genre — BUG-04 causes failure | BUG-04 |
| TC-F8 | Direct URL navigation | Negative | App should load — BUG-01 causes 404 | BUG-01 |

### Pagination Tests (`PaginationTests.java`)

| TC ID | Title | Type | Expected Result | Bug |
|---|---|---|---|---|
| TC-P1 | Next / Previous pagination | Positive | Page changes, Previous returns to original | — |
| TC-P2 | Last page edge case | Negative | Last page should load — BUG-02 shows error | BUG-02 |

---

## Test Design Techniques

- **Equivalence Partitioning** — Valid and invalid input classes for filters (e.g. Year 2019–2021 as valid, Year 1800 as invalid; rating 1 vs 5 as boundary classes; search term with known results vs. gibberish input)
- **Boundary Value Analysis** — Testing first and last pages of pagination; year range boundaries; minimum and maximum star rating values (1 and 5)
- **State Transition Testing** — Category switching verifies the app transitions correctly between states (Popular → Trend → Newest → Top Rated)
- **Error Guessing** — Direct URL access, last page navigation, and filter combinations targeted based on common SPA (React) failure points

---

## Design Patterns

- **Page Object Model (POM)** — All UI interactions are encapsulated in `HomePage.java`. Test classes call page methods only — no locators exist in test files.
- **Factory Pattern** — `DriverFactory` uses `ThreadLocal<WebDriver>` to create and manage browser instances, making the design parallel-execution ready.
- **Base Test Pattern** — `BaseTest.java` centralises `@BeforeSuite`, `@BeforeMethod`, `@AfterMethod` lifecycle management, ExtentReports initialisation, and screenshot capture.

---

## Browser API (CDP) Usage

Selenium 4's Chrome DevTools Protocol is used to monitor browser-level network activity during test execution.

**How it works:**
- `DriverFactory` enables Chrome performance logging via `ChromeOptions`
- During pagination tests, network requests triggered by clicking Next/Previous are captured from the browser performance log
- Captured network events (URLs, status codes) are written to `reports/networkLogs/` for traceability

> Note: Full CDP DevTools API (v142) was tested but commented out due to Chrome version dependency. The performance log approach is version-agnostic and works across Chrome versions.

---

## Logging

Java Util Logging (JUL) is used throughout:

- Every page action logs an `INFO` entry before and after execution
- Warnings are logged for non-critical issues (e.g. known app bugs detected)
- Severe errors are logged on unexpected exceptions
- Logs appear in the **console** during execution and are also attached to the **ExtentReports HTML report** as test step details

---

## Reporting

ExtentReports generates a self-contained HTML report at `target/extent-report.html`.

The report includes:
- Pass / Fail / Warning / Skip status per test
- Step-by-step logs with timestamps
- Screenshots automatically attached on test failure
- Browser console log output captured per test
- Network log file paths for traceability

---

## CI Integration Approach

> Implementation is not included but the approach is documented here.

### Tool: GitHub Actions

A `.github/workflows/ci.yml` workflow would trigger on every `push` and `pull_request` to `main`:

```yaml
name: QA Automation CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Install Chrome
        uses: browser-actions/setup-chrome@latest

      - name: Run tests in headless mode
        run: mvn clean test -Dheadless=true

      - name: Upload HTML report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: extent-report
          path: target/extent-report.html

      - name: Upload network logs
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: network-logs
          path: reports/networkLogs/
```

### Key CI Decisions
- `headless=true` flag enables Chrome to run without a display on Linux CI agents
- `if: always()` ensures the report is uploaded even when tests fail
- WebDriverManager eliminates the need to pre-install ChromeDriver on the CI agent
- Maven Surefire plugin reads `testng.xml` — no CI-specific configuration needed

---

## Defects Found

| Bug ID | Title | Steps to Reproduce | Expected | Actual | Severity | Status |
|---|---|---|---|---|---|---|
| BUG-01 | Direct URL `/popular` and `/top` shows 404 | Navigate directly to `https://tmdb-discover.surge.sh/popular` | App loads with results | Surge.sh "Page Not Found" displayed | High | Open |
| BUG-02 | Last page pagination shows error | Go to Popular → click last page number (57,034) | Results load normally | "Something went wrong! Please try again later." displayed | High | Open |
| BUG-03 | Year filter returns out-of-range results | Set Year filter to 2019–2021 → observe results | All results within 2019–2021 | Results from other years included | Medium | Open |
| BUG-04 | Genre filter returns mixed genres | Select Genre = Action → observe result genres | All results are Action | Results include Horror, Thriller, Animation etc. | Medium | Open |