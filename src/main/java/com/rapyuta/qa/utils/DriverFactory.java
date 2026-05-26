package com.rapyuta.qa.utils;

import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverFactory {

    private static final Logger log = Logger.getLogger(DriverFactory.class.getName());
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver createDriver() {
        if (driver.get() == null) {
            log.info("Initializing Chrome WebDriver...");
            ChromeOptions options = new ChromeOptions();
            if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
                options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            }
            driver.set(new ChromeDriver(options));  // set instance INTO the ThreadLocal
            driver.get().manage().window().maximize();
            log.info("Chrome WebDriver initialized and browser window maximized.");
        }
        return driver.get();
    }

    public static void quitDriver() {
        WebDriver webDriver = driver.get();   // get the actual WebDriver instance
        if (webDriver != null) {
            log.info("Quitting WebDriver and closing browser...");
            webDriver.quit();                 // quit on the instance
            driver.remove();                  // remove from ThreadLocal
            log.info("WebDriver quit successfully.");
        }
    }
}