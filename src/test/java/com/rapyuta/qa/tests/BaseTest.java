package com.rapyuta.qa.tests;

import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.rapyuta.qa.utils.DriverFactory;

/**
 * BaseTest — lifecycle management for all test classes.
 */
public class BaseTest {

    protected static final Logger log = Logger.getLogger(BaseTest.class.getName());

    protected static ExtentReports extent;
    protected ExtentTest test;
    protected WebDriver driver;

    // ThreadLocal so parallel classes don't share the ExtentTest reference
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();
    // Tracks the placeholder node name so startTest() can detect and rename it
    private String placeholderName = null;

    @BeforeSuite(alwaysRun = true)
    public synchronized void setUpSuite() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report.html");
            spark.config().setReportName("TMDB Regression Suite");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            log.info("ExtentReports initialised.");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(java.lang.reflect.Method method) {
        // webDriverManager times out on restricted networks because it tries
        // to resolve the ChromeDriver version from the internet. Setting a short
        // timeout and forcing it to use any locally cached driver prevents
        // HttpConnectTimeoutException from skipping all FilterTests at setUp().
        io.github.bonigarcia.wdm.WebDriverManager.chromedriver()
            .timeout(10)
            .avoidResolutionCache()
            .setup();
        driver = DriverFactory.createDriver();
        if (driver == null) {
            Assert.fail("WebDriver could not be created — setUp failed for: " + method.getName());
        }

        // create a placeholder node immediately so tearDown always has
        // something to write to, even if the test method body is never entered.
        // We store the node in 'test' — startTest() will rename it rather than
        // creating a second node, eliminating the ghost "[placeholder]" pass entries.
        placeholderName = method.getName() + " [placeholder]";
        test = extent.createTest(placeholderName);
        testThread.set(test);

        log.info("WebDriver ready for: " + method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            // Use the node created by startTest() (or the placeholder from setUp).
            // If somehow still null, create a last-resort node.
            if (test == null) {
                test = extent.createTest(result.getName());
                test.warning("startTest() was not called — test node created in tearDown.");
            }

            if (result.getStatus() == ITestResult.FAILURE) {
                log.warning("Test FAILED: " + result.getName());

                try {
                    String base64 = ((org.openqa.selenium.TakesScreenshot) driver)
                            .getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
                    test.addScreenCaptureFromBase64String(base64, "Failure — " + result.getName());
                } catch (Exception e) {
                    test.warning("Screenshot could not be captured: " + e.getMessage());
                }

                try {
                    String consoleLogs = captureConsoleLogs();
                    if (consoleLogs != null && !consoleLogs.isEmpty()) {
                        test.info("Browser Console Logs:\n" + consoleLogs);
                    }
                } catch (Exception e) {
                    log.warning("Console log capture failed: " + e.getMessage());
                }

                test.fail(result.getThrowable());

            } else if (result.getStatus() == ITestResult.SUCCESS) {
                test.pass("Test passed.");

            } else if (result.getStatus() == ITestResult.SKIP) {
                // FIX I: Mark skipped tests properly in the report with the reason.
                String skipReason = result.getThrowable() != null
                        ? result.getThrowable().getMessage()
                        : "No reason provided by TestNG.";
                test.skip("Test skipped: " + skipReason);
                log.warning("Test SKIPPED: " + result.getName() + " — " + skipReason);
            }

        } catch (Exception e) {
            log.log(java.util.logging.Level.SEVERE, "Unexpected error in tearDown", e);
        } finally {
            if (driver != null) {
                DriverFactory.quitDriver();
                driver = null;
            }
            test = null;
            testThread.remove();
        }
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed.");
        }
    }

    /**
     * Call as the FIRST line of every test method.
     * Creates (or overwrites the placeholder) ExtentTest node for this test.
     * Idempotent — safe to call after setUp() has already set a placeholder.
     */
    protected void startTest(String testName) {
        if (test != null && placeholderName != null && test.getModel().getName().equals(placeholderName)) {
            // Rename the existing placeholder node instead of creating a second one.
            // This prevents a ghost "[placeholder]" pass node appearing alongside the real test.
            test.getModel().setName(testName);
        } else {
            test = extent.createTest(testName);
            testThread.set(test);
        }
        placeholderName = null;
    }

    protected void takeScreenshot(String label) {
        try {
            String base64 = ((org.openqa.selenium.TakesScreenshot) driver)
                    .getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
            test.addScreenCaptureFromBase64String(base64, label);
        } catch (Exception e) {
            log.warning("Screenshot '" + label + "' failed: " + e.getMessage());
        }
    }

    protected String captureConsoleLogs() {
        try {
            LogEntries logEntries = driver.manage().logs().get("browser");
            StringBuilder sb = new StringBuilder();
            for (LogEntry entry : logEntries) {
                if (entry.getMessage().contains("favicon.ico")) continue;
                sb.append(entry.getLevel())
                  .append(": ")
                  .append(entry.getMessage())
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warning("Could not capture console logs: " + e.getMessage());
            return "";
        }
    }
}