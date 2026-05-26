package com.rapyuta.qa.tests;

import java.time.Duration;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.rapyuta.qa.utils.DriverFactory;

public class BaseTest {
    protected WebDriver driver;
    protected static ExtentReports extent;
    protected ExtentTest test;
    protected static final Logger log = Logger.getLogger(BaseTest.class.getName());

    @BeforeSuite
    public void beforeSuite() {
    	log.info("Initializing ExtentReports...");
    	ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report.html");
    	spark.config().setDocumentTitle("Rapyuta QA Automation Report");
    	extent = new ExtentReports();
    	extent.attachReporter(spark);
    	log.info("ExtentReports setup completed successfully.");
    }

    @AfterSuite
    public void afterSuite() {
        if (extent != null) {
        	log.info("Flushing ExtentReports...");
        extent.flush();}
        log.info("Test suite execution completed.");
    }

    @BeforeMethod
    public void setUp() {
    	log.info("Setting up WebDriver instance...");
        driver = DriverFactory.createDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        log.info("WebDriver setup complete.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (test == null) {
                test = extent.createTest(result.getName());
                test.info("Test node initialized in tearDown.");
            }

            if (result.getStatus() == ITestResult.FAILURE) {
                log.warning("Test FAILED: " + result.getName());

                // 1. Attach screenshot separately — do NOT call test.fail() inside takeScreenshot()
                try {
                    String base64 = ((org.openqa.selenium.TakesScreenshot) driver)
                        .getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
                    test.addScreenCaptureFromBase64String(base64, "Failure - " + result.getName());
                    log.info("Screenshot attached for: " + result.getName());
                } catch (Exception e) {
                    log.warning("Screenshot failed: " + e.getMessage());
                    test.warning("Screenshot could not be captured: " + e.getMessage());
                }

                // 2. Attach browser console logs
                try {
                    String consoleLogs = captureConsoleLogs();
                    if (consoleLogs != null && !consoleLogs.isEmpty()) {
                        test.info("Browser Console Logs:\n" + consoleLogs);
                    }
                } catch (Exception e) {
                    log.warning("Console log capture failed: " + e.getMessage());
                }

                // 3. Mark test as failed with the exception
                test.fail(result.getThrowable());

            } else if (result.getStatus() == ITestResult.SUCCESS) {
                test.pass("Test passed");

            } else if (result.getStatus() == ITestResult.SKIP) {
                test.skip("Test skipped");
            }

        } catch (Exception e) {
            log.log(java.util.logging.Level.SEVERE, "Unexpected error in tearDown", e);
        } finally {
            if (driver != null) {
                DriverFactory.quitDriver();
            }
        }
    }

    protected void takeScreenshot(String testName) {
        if (driver == null) {
            log.warning("Driver null — skipping screenshot for: " + testName);
            return;
        }
        try {
            String base64 = ((org.openqa.selenium.TakesScreenshot) driver)
                .getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
            test.addScreenCaptureFromBase64String(base64, testName);
            log.info("Screenshot attached: " + testName);
        } catch (Exception e) {
            log.warning("Screenshot failed: " + e.getMessage());
        }
    }

    protected void startTest(String name) {
        log.info("Starting test: " + name);
        if (extent == null) {
            log.severe("ExtentReports is null — reinitializing...");
            ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report.html");
            spark.config().setDocumentTitle("Rapyuta QA Automation Report");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        test = extent.createTest(name);
        log.info("Test node created: " + name);
    }
    
    protected String captureConsoleLogs() {
        try {
            LogEntries logEntries = driver.manage().logs().get("browser");
            StringBuilder logBuilder = new StringBuilder();
            for (LogEntry entry : logEntries) {
                // Skip favicon 404 — known noise from demo site
                if (entry.getMessage().contains("favicon.ico")) {
                    continue;
                }
                logBuilder.append(entry.getLevel())
                          .append(": ")
                          .append(entry.getMessage())
                          .append("\n");
            }
            return logBuilder.toString().trim();
        } catch (Exception e) {
            log.warning("Could not capture console logs: " + e.getMessage());
            return "";
        }
    }

}
