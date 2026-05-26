package com.rapyuta.qa.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class HomePage {
    private WebDriver driver;
    private static final Logger log = Logger.getLogger(HomePage.class.getName());
    private String url = "https://tmdb-discover.surge.sh/";

    private By categoryLinks = By.xpath("//nav//li"); // categories at top
    private By resultTitles = By.cssSelector("p.text-blue-500.font-bold"); // card selector
    private By nextButton = By.cssSelector("a[aria-label='Next page']"); 
    private By prevButton = By.cssSelector("a[aria-label='Previous page']");
    
 // Filter locators
    private By titleSearchInput = By.cssSelector("input[name='search']");

    // React Select containers (by order of appearance)
    private By typeSelectInput     = By.id("react-select-2-input");
    private By genreSelectInput    = By.id("react-select-3-input");
    private By yearFromSelectInput = By.id("react-select-4-input");
    private By yearToSelectInput   = By.id("react-select-5-input");

    // Rating stars
    private By ratingStars = By.cssSelector("li.rc-rate-star");
    
    private By cardSubtitle = By.cssSelector("p.text-gray-400, p.text-sm, .card-subtitle");
    
    private By errorMessage = By.xpath("//*[contains(text(),'Something went wrong')]");
    
    public boolean isErrorDisplayed() {
        return !driver.findElements(errorMessage).isEmpty();
    }
    
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
        log.info("Waiting for results to change from: " + oldFirstTitle);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // increase timeout
        wait.until(d -> {
            List<String> titles = driver.findElements(resultTitles)
                    .stream()
                    .map(e -> e.getText().trim())
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());
            return !titles.isEmpty() && !titles.get(0).equals(oldFirstTitle);
        });
        log.info("Results changed successfully.");
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
    	driver.findElement(prevButton).click(); 
    	}

    public List<String> getResultTypes() {
        log.info("Extracting result type labels...");
        By typeLabel = By.cssSelector(".card .type, .badge, span.media-type");
        return driver.findElements(typeLabel).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }
    
    public void selectReactDropdown(By inputLocator, String value) {
        log.info("Selecting React dropdown value: " + value);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Step 1: Get the input element
        WebElement input = driver.findElement(inputLocator);

        // Step 2: Click the parent control div to open the dropdown
        // React Select input is nested inside the control — go up to the control
        WebElement control = (WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("return arguments[0].closest('[class*=\"control\"]')", input);
        control.click();
        log.info("Opened React dropdown for: " + value);

        // Step 3: Type to filter options
        input.sendKeys(value);

        // Step 4: Wait for matching option and click it
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(@class,'option') and normalize-space(text())='" + value + "']")));
        option.click();
        log.info("Selected React dropdown option: " + value);
    }

    public void selectType(String type) {
        log.info("Selecting type filter: " + type);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement input = driver.findElement(typeSelectInput);
        WebElement control = (WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("return arguments[0].closest('[class*=\"control\"]')", input);
        control.click();

        // Use contains() instead of exact match to handle variations
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(@class,'option') and contains(text(),'" + type + "')]")));
        option.click();
        log.info("Type selected: " + type);
    }

    public void searchByTitle(String title) {
        log.info("Searching for title: " + title);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement searchBox = wait.until(
            ExpectedConditions.visibilityOfElementLocated(titleSearchInput));
        searchBox.clear();
        searchBox.sendKeys(title);
        // Search triggers on input — no Enter/button needed
        new WebDriverWait(driver, Duration.ofSeconds(5))
            .until(ExpectedConditions.stalenessOf(
                driver.findElements(resultTitles).isEmpty() ? searchBox
                    : driver.findElements(resultTitles).get(0)));
        log.info("Title search submitted: " + title);
    }
    
    public void selectGenre(String genre) {
        log.info("Selecting genre: " + genre);
        selectReactDropdown(genreSelectInput, genre);
    }

    public void setRating(int stars) {
        log.info("Setting rating: " + stars + " stars");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> starElements = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(ratingStars));
        starElements.get(stars - 1)
                    .findElement(By.cssSelector(".rc-rate-star-second"))
                    .click();
        log.info("Rating set to: " + stars + " stars and above");
    }
    
    public List<String> getResultGenres() {
        log.info("Extracting genre labels from result cards...");
        try {
            List<WebElement> subtitles = driver.findElements(cardSubtitle);
            return subtitles.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(t -> t.contains(","))
                .map(t -> t.split(",")[0].trim()) // "Action, 2022" → "Action"
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warning("Could not extract genres: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    public void setYearRange(String from, String to) {
        log.info("Setting year range: " + from + " to " + to);
        selectReactDropdownByScroll(yearFromSelectInput, from);
        log.info("Year From set to: " + from);
        selectReactDropdownByScroll(yearToSelectInput, to);
        log.info("Year To set to: " + to);
    }

    private void selectReactDropdownByScroll(By inputLocator, String value) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Open the dropdown by clicking the control
        WebElement input = driver.findElement(inputLocator);
        WebElement control = (WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("return arguments[0].closest('[class*=\"control\"]')", input);
        control.click();

        // Type the value to jump to it in the list
        input.sendKeys(value);

        // Wait for option to appear and click — use contains for safety
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'option') and normalize-space(text())='" + value + "']")));
        option.click();
        log.info("Dropdown option selected: " + value);
    }
    
    public boolean isPageError() {
        List<By> errorLocators = List.of(
            By.xpath("//*[contains(text(),'Page Not Found')]"),
            By.xpath("//*[contains(text(),'404')]"),
            By.xpath("//*[contains(text(),'Something went wrong')]"),
            By.xpath("//*[contains(text(),'Cannot GET')]"),
            By.xpath("//*[contains(text(),'powered by')]") // ← Surge.sh 404 page footer
        );
        return errorLocators.stream()
            .anyMatch(loc -> !driver.findElements(loc).isEmpty());
    }
    
    public List<String> getResultYears() {
        log.info("Extracting release years from result cards...");
        try {
            List<WebElement> subtitles = driver.findElements(cardSubtitle);
            return subtitles.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(t -> t.contains(","))
                .map(t -> {
                    String[] parts = t.split(",");
                    return parts.length > 1 ? parts[parts.length - 1].trim() : "";
                })
                .filter(y -> y.matches("\\d{4}")) // only valid 4-digit years
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warning("Could not extract years: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void clickLastVisiblePage() {
        log.info("Finding last visible page number in pagination...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for pagination to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("ul li a[aria-label*='Page']")));

        // Get all page number links using aria-label pattern
        List<WebElement> pageLinks = driver.findElements(
            By.cssSelector("ul li a[aria-label*='Page']"));

        if (pageLinks.isEmpty()) {
            log.warning("No page links found with aria-label pattern.");
            return;
        }

        // Last element in the list is the highest page number
        WebElement lastPage = pageLinks.get(pageLinks.size() - 1);
        String pageNum = lastPage.getAttribute("aria-label");
        log.info("Clicking last visible page: " + pageNum);

        // Scroll into view then click
        ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView(true);", lastPage);
        lastPage.click();

        log.info("Clicked last page: " + pageNum);
    }
}
