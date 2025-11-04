package com.rapyuta.qa.tests;

import java.time.Duration;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
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
    private static final Logger log = Logger.getLogger(BaseTest.class.getName());

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
        	log.info("Finalizing test: " + result.getName());
            if (result.getStatus() == ITestResult.FAILURE) {
            	log.warning("Test FAILED: " + result.getName());
            	String consoleLogs = captureConsoleLogs();
                test.info("Browser Console Logs:\n" + consoleLogs);
                takeScreenshot(result.getName());
                test.fail(result.getThrowable());
            } else if (result.getStatus() == ITestResult.SUCCESS) {
            	log.info("Test passed");
                test.pass("Test passed");
            } else {
            	log.info("Test skipped");
                test.skip("Test skipped");
            }
        } catch (Exception e) {
        	log.log(Level.SEVERE, "Error during tearDown", e);
        } finally {
            if (driver != null) {
            	log.info("Quitting WebDriver...");
            	DriverFactory.quitDriver();}
        }
    }

    protected void startTest(String name) {
    	log.info("Starting test: " + name);
        test = extent.createTest(name);
    }

    protected void takeScreenshot(String name) {
    	log.info("Capturing screenshot for: " + name);
        String base64Screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        test.addScreenCaptureFromBase64String(base64Screenshot, name);
        log.info("Screenshot added to report for: " + name);
    }

    
    public String captureConsoleLogs() {
        LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
        StringBuilder logBuilder = new StringBuilder();

        for (org.openqa.selenium.logging.LogEntry entry : logEntries) {
            logBuilder.append(new Date())
                      .append(" - ")
                      .append(entry.getLevel())
                      .append(" - ")
                      .append(entry.getMessage())
                      .append("\n");
            log.info("Browser console logs captured successfully.");
        }
        return logBuilder.toString();
    }

}
