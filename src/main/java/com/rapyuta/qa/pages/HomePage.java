package com.rapyuta.qa.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * HomePage — Page Object Model for https://tmdb-discover.surge.sh/
 */
public class HomePage {

    private final WebDriver driver;
    private static final Logger log = Logger.getLogger(HomePage.class.getName());
    private final String BASE_URL = "https://tmdb-discover.surge.sh/";

    // --- Locators ---
    private final By resultTitles    = By.cssSelector("p.text-blue-500.font-bold");
    private final By nextButton      = By.cssSelector("a[aria-label='Next page']");
    private final By prevButton      = By.cssSelector("a[aria-label='Previous page']");
    private final By titleSearchInput = By.cssSelector("input[name='search']");
    private final By typeSelectInput  = By.id("react-select-2-input");
    private final By genreSelectInput = By.id("react-select-3-input");
    private final By yearFromInput    = By.id("react-select-4-input");
    private final By yearToInput      = By.id("react-select-5-input");
    private final By cardSubtitle     = By.cssSelector("p.text-gray-400, p.text-sm, .card-subtitle");
    private final By errorMessage     = By.xpath("//*[contains(text(),'Something went wrong')]");

    public HomePage(WebDriver driver) {
        this.driver = driver;
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * navigate to base URL and wait for the first result card to appear.
     * Prevents tests from racing ahead before React renders data.
     */
    public void open() {
        log.info("Navigating to: " + BASE_URL);
        driver.get(BASE_URL);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(resultTitles));
        log.info("Home page ready — results visible.");
    }

    /**
     * use contains(normalize-space(.), 'name') so that:
     *   - "Trend" matches the rendered text "Trending"
     *   - Leading/trailing whitespace in text nodes is ignored
     *   - "Top Rated", "Newest", "Popular" all continue to work
     *
     * after clicking, wait for the result list to go stale (old cards
     *        removed) and then for new cards to appear, so the caller always
     *        reads fresh results.
     */
    public void clickCategory(String name) {
        log.info("Clicking category: " + name);

        // contains + normalize-space instead of exact text()='...'
        By locator = By.xpath(
                "//nav//li[contains(normalize-space(.), '" + name + "')]");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement categoryEl = wait.until(ExpectedConditions.elementToBeClickable(locator));
        log.info("Found category element: '" + categoryEl.getText().trim() + "'");
        categoryEl.click();

        // wait for stale + re-render so results belong to the clicked category
        waitForResultsToReload();
        log.info("Category '" + name + "' loaded.");
    }

    // -------------------------------------------------------------------------
    // Filters
    // -------------------------------------------------------------------------

    public void selectType(String type) {
        log.info("Selecting type filter: " + type);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement input   = driver.findElement(typeSelectInput);
        WebElement control = getReactSelectControl(input);
        control.click();

        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(@class,'option') and contains(normalize-space(text()),'"
                        + type + "')]")));
        option.click();

        waitForResultsToReload();
        log.info("Type filter set: " + type);
    }

    public void selectGenre(String genre) {
        log.info("Selecting genre filter: " + genre);
        selectReactDropdown(genreSelectInput, genre);
        waitForResultsToReload();
    }

    public void setYearRange(String from, String to) {
        log.info("Setting year range: " + from + " → " + to);
        selectReactDropdownByScroll(yearFromInput, from);
        log.info("Year From set: " + from);
        selectReactDropdownByScroll(yearToInput, to);
        log.info("Year To set: " + to);
        waitForResultsToReload();
    }

    /**
     * Clicks the Nth star (1–5) in the rc-rate component.
     */
    public void setRating(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + stars);
        }
     
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("ul.rc-rate")));
     
        By starLocator = By.cssSelector(
                "ul.rc-rate li div[aria-posinset='" + stars + "'] .rc-rate-star-second");
     
        WebElement star = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(starLocator));
     
        // Scroll the star into the centre of the viewport to minimise overlap risk
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});", star);
     
        // JS click bypasses the intercepting overlay element
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", star);
     
        log.info("Rating set to " + stars + " star(s).");
        waitForResultsToReload();
    }

    /**
     * robust search — waits for input, clears, types, then waits for
     * the result list to actually change rather than relying on Thread.sleep.
     */
    public void searchByTitle(String title) {
        log.info("Searching for title: " + title);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement searchBox = wait.until(
                ExpectedConditions.elementToBeClickable(titleSearchInput));
        searchBox.clear();
        searchBox.sendKeys(title);

        // wait for result list to refresh — either stale cards disappear or count changes
        waitForResultsToReload();
        log.info("Search submitted: " + title);
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    public void clickNext() {
        log.info("Clicking Next page...");
        driver.findElement(nextButton).click();
    }

    public void clickPrev() {
        log.info("Clicking Previous page...");
        driver.findElement(prevButton).click();
    }

    public void clickLastVisiblePage() {
        log.info("Locating last visible page number...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("ul li a[aria-label*='Page']")));

        List<WebElement> pageLinks = driver.findElements(
                By.cssSelector("ul li a[aria-label*='Page']"));

        if (pageLinks.isEmpty()) {
            log.warning("No page links found — pagination may not be rendered.");
            return;
        }

        WebElement lastPage = pageLinks.get(pageLinks.size() - 1);
        String ariaLabel = lastPage.getAttribute("aria-label");
        log.info("Clicking last visible page: " + ariaLabel);

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", lastPage);
        lastPage.click();
        log.info("Last page clicked: " + ariaLabel);
    }

    public void waitForResultsToChange(String oldFirstTitle) {
        log.info("Waiting for results to change from: '" + oldFirstTitle + "'");
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> {
                    List<String> titles = d.findElements(resultTitles)
                            .stream()
                            .map(e -> e.getText().trim())
                            .filter(t -> !t.isEmpty())
                            .collect(Collectors.toList());
                    return !titles.isEmpty() && !titles.get(0).equals(oldFirstTitle);
                });
        log.info("Results changed successfully.");
    }

    // -------------------------------------------------------------------------
    // Data extraction
    // -------------------------------------------------------------------------

    public int getResultCount() {
        int count = driver.findElements(resultTitles).size();
        log.info("Result count: " + count);
        return count;
    }

    public List<String> getResultTitles() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<String> titles = new ArrayList<>();
        int attempt = 0;
        while (attempt < 3) {
            try {
                List<WebElement> elements = wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(resultTitles));
                titles = elements.stream()
                        .map(WebElement::getText)
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());
                if (!titles.isEmpty()) {
                    log.info("Fetched " + titles.size() + " result titles.");
                    break;
                }
            } catch (StaleElementReferenceException e) {
                log.warning("Stale element on getResultTitles() attempt " + (attempt + 1));
            }
            attempt++;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return titles;
    }

    public List<String> getResultGenres() {
        log.info("Extracting genre labels from cards...");
        try {
            return driver.findElements(cardSubtitle).stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(t -> t.contains(","))
                    .map(t -> t.split(",")[0].trim())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warning("Could not extract genres: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<String> getResultYears() {
        log.info("Extracting release years from cards...");
        try {
            return driver.findElements(cardSubtitle).stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(t -> t.contains(","))
                    .map(t -> {
                        String[] parts = t.split(",");
                        return parts.length > 1 ? parts[parts.length - 1].trim() : "";
                    })
                    .filter(y -> y.matches("\\d{4}"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warning("Could not extract years: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // -------------------------------------------------------------------------
    // State checks
    // -------------------------------------------------------------------------

    public boolean isErrorDisplayed() {
        return !driver.findElements(errorMessage).isEmpty();
    }

    public boolean isPageError() {
        List<By> errorLocators = List.of(
                By.xpath("//*[contains(text(),'Page Not Found')]"),
                By.xpath("//*[contains(text(),'404')]"),
                By.xpath("//*[contains(text(),'Something went wrong')]"),
                By.xpath("//*[contains(text(),'Cannot GET')]"),
                By.xpath("//*[contains(text(),'powered by')]")
        );
        return errorLocators.stream()
                .anyMatch(loc -> !driver.findElements(loc).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Shared React Select interaction: open control → type value → click option.
     */
    private void selectReactDropdown(By inputLocator, String value) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement input   = driver.findElement(inputLocator);
        WebElement control = getReactSelectControl(input);
        control.click();
        input.sendKeys(value);
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(@class,'option') and normalize-space(text())='" + value + "']")));
        option.click();
        log.info("React dropdown selected: " + value);
    }

    /**
     * React Select year dropdowns use a scrollable list rather than type-ahead,
     * so we open → type to jump → click the exact match.
     */
    private void selectReactDropdownByScroll(By inputLocator, String value) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement input   = driver.findElement(inputLocator);
        WebElement control = getReactSelectControl(input);
        control.click();
        input.sendKeys(value);
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'option') and normalize-space(text())='" + value + "']")));
        option.click();
        log.info("Year dropdown selected: " + value);
    }

    /**
     * Navigate up from the hidden React Select input to its visible control div.
     */
    private WebElement getReactSelectControl(WebElement input) {
        return (WebElement) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].closest('[class*=\"control\"]')", input);
    }

    /**
     * Wait for the current result cards to go stale (DOM update) and then for
     * new cards to appear. Used after every action that triggers a data reload.
     * Falls back gracefully if no cards were present before the action.
     */
    private void waitForResultsToReload() {
        try {
            List<WebElement> currentCards = driver.findElements(resultTitles);
            if (!currentCards.isEmpty()) {
                // Wait for the first card to go stale (React re-renders the list)
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.stalenessOf(currentCards.get(0)));
            }
            // Then wait for fresh cards to appear
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(resultTitles));
        } catch (Exception e) {
            log.warning("waitForResultsToReload timeout — proceeding: " + e.getMessage());
        }
    }
}