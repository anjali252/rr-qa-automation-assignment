package com.rapyuta.qa.utils;

import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory {

    private static WebDriver driver;
    private static final Logger log = Logger.getLogger(DriverFactory.class.getName());
    public static WebDriver createDriver() {
        if (driver == null) {
        	log.info("Initializing Chrome WebDriver...");
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            log.info("Chrome WebDriver initialized and browser window maximized.");
        }
        return driver;
    }
    
    public static void quitDriver() {
        if (driver != null) {
        	log.info("Quitting WebDriver and closing browser...");
            driver.quit();
            driver = null;
            log.info("WebDriver quit successfully.");
        }
    }
    
    
}