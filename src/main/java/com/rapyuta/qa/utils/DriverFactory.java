package com.rapyuta.qa.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

/**
 * DriverFactory — ThreadLocal WebDriver management
 */
public class DriverFactory {

    private static final Logger log = Logger.getLogger(DriverFactory.class.getName());

    // ThreadLocal ensures each parallel thread gets its own WebDriver instance.
    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    public static WebDriver createDriver() {
        // quit and remove any leftover instance before creating a new one.
        if (driverThread.get() != null) {
            log.warning("Stale WebDriver found in ThreadLocal — quitting before creating new one.");
            try {
                driverThread.get().quit();
            } catch (Exception ignored) {
                // Already dead — ignore
            }
            driverThread.remove();
        }

        log.info("Initializing Chrome WebDriver...");
        ChromeOptions options = new ChromeOptions();

        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }

        // enable browser and performance logging so captureConsoleLogs() works.
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        logPrefs.enable(LogType.PERFORMANCE, Level.INFO);
        options.setCapability("goog:loggingPrefs", logPrefs);

        // Suppress unwanted Chrome noise in console
        options.addArguments("--log-level=3");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-logging"});

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driverThread.set(driver);

        log.info("Chrome WebDriver initialized. Thread: " + Thread.currentThread().getId());
        return driver;
    }

    public static WebDriver getDriver() {
        return driverThread.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverThread.get();
        if (driver != null) {
            log.info("Quitting WebDriver. Thread: " + Thread.currentThread().getId());
            try {
                driver.quit();
            } catch (Exception e) {
                log.warning("Exception during driver.quit(): " + e.getMessage());
            } finally {
                driverThread.remove();
            }
            log.info("WebDriver quit and removed from ThreadLocal.");
        }
    }
}