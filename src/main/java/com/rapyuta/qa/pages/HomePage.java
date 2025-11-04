package com.rapyuta.qa.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.logging.Logger;

public class HomePage {
    private WebDriver driver;
    private static final Logger log = Logger.getLogger(HomePage.class.getName());
    private String url = "https://tmdb-discover.surge.sh/";

    private By categoryLinks = By.xpath("//nav//li"); // categories at top
    private By resultTitles = By.cssSelector("p.text-blue-500.font-bold"); // card selector
    private By nextButton = By.cssSelector("a[aria-label='Next page']"); 
    private By prevButton = By.cssSelector("a[aria-label='Previous page']");
    private By releaseYear = By.cssSelector(".card .meta");

    public HomePage(WebDriver d) { this.driver = d; }
    public void open() { log.info("Navigating to URL: " + url);
    driver.get(url); 
    log.info("Page opened successfully: " + driver.getCurrentUrl());
    }

    public void clickCategory(String name) {
    	log.info("Attempting to click category: " + name);
    	
        List<WebElement> cats = driver.findElements(categoryLinks);
        for (WebElement e : cats) {
            if (e.getText().trim().equalsIgnoreCase(name)) {
            	log.info("Clicking category link: " + e.getText());
                e.click();
                return;
            }
        }
        String msg = "Category not found: " + name;
        log.severe(msg);
        throw new RuntimeException(msg);
    }

    public List<String> getResultTitles() {
    	WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<String> titles = new ArrayList<>();
        int retry = 0;
        log.info("Fetching result titles from page...");
        while (retry < 3) {
            try {
            	List<WebElement> elements =wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(resultTitles));
            	titles = elements.stream()
                        .map(WebElement::getText)
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());
                if (!titles.isEmpty()) {log.info("Successfully fetched " + titles.size() + " titles.");
                break; // success
                }
            } catch (StaleElementReferenceException ignored) {
                log.warning("Retrying getResultTitles(), attempt " + (retry + 1));
            }
            retry++;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return titles;
    }

    
    public void waitForResultsToChange(String oldFirstTitle) {
    	log.info("Waiting for results to change from previous title: " + oldFirstTitle);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            wait.until(d -> {
                List<String> titles = driver.findElements(resultTitles)
                        .stream()
                        .map(e -> e.getText().trim())
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());
                return !titles.isEmpty() && !titles.get(0).equals(oldFirstTitle);
            });
        } catch (Exception e) {
        	log.warning("Timed out waiting for results to change: " + e.getMessage());
        }
    }

    public int getResultCount() {
    	int count = driver.findElements(resultTitles).size();
    	log.info("Number of result cards on current page: " + count);
        return count;
    }

    public void clickNext() { log.info("Clicking 'Next' button...");
    driver.findElement(nextButton).click(); }

    public void clickPrev() { 
    	log.info("Clicking 'Previous' button...");
    	driver.findElement(prevButton).click(); }

    public List<String> getResultYears() {
    	log.info("Extracting release years for displayed results...");
        return driver.findElements(resultTitles).stream()
                .map(card -> {
                    try {
                        String t = card.findElement(releaseYear).getText();
                        return t.replaceAll("[^0-9]", "").trim();
                    } catch (Exception ex) {
                        return "";
                    }
                }).collect(Collectors.toList());
    }
}
