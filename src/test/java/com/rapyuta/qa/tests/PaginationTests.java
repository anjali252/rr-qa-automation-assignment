package com.rapyuta.qa.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.rapyuta.qa.pages.HomePage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * PaginationTests — TC-P1 and TC-P2.
 */
public class PaginationTests extends BaseTest {

    // Known app-level noise: these URLs fire on every page load and are
    // unrelated to the test being executed. Filtering them prevents false
    // warnings on tests that otherwise pass cleanly.
    private static final List<String> KNOWN_NOISE_URLS = List.of(
        "api.themoviedb.org/3/genre/movie/list",
        "api.themoviedb.org/3/genre/tv/list",
        "tmdb-discover.surge.sh/static/js/2.a392af60.chunk.js"
    );

    // -------------------------------------------------------------------------
    // TC-P1
    // -------------------------------------------------------------------------

    @Test(description = "TC-P1: Verify Next/Previous pagination changes and restores results.")
    public void nextPrevPagination() throws InterruptedException, IOException {
        startTest("TC-P1 - Next/Previous Pagination");
        HomePage home = new HomePage(driver);
        home.open();

        List<String> networkEvents = new ArrayList<>();

        home.clickCategory("Trend");
        test.info("Navigated to Trending category.");

        List<String> firstPageTitles = home.getResultTitles();
        test.info("Results on page 1: " + firstPageTitles.size());

        // capture browser logs but filter out known app-level noise before
        // deciding whether to emit a warning. The genre API and chunk.js errors
        // appear on every load — they say nothing about pagination correctness.
        LogEntries browserLogs = driver.manage().logs().get("browser");
        for (LogEntry entry : browserLogs) {
            String msg = entry.getMessage();
            if (entry.getLevel().equals(Level.SEVERE)
                    && !msg.contains("favicon.ico")
                    && KNOWN_NOISE_URLS.stream().noneMatch(msg::contains)) {
                test.warning("Unexpected browser error: " + msg);
            }
        }

        Assert.assertFalse(firstPageTitles.isEmpty(),
                "Expected results on page 1 after navigating to Trending.");

        String firstTitlePage1 = firstPageTitles.get(0);
        test.info("First title on page 1: " + firstTitlePage1);

        // --- Click Next ---
        home.clickNext();
        home.waitForResultsToChange(firstTitlePage1);
        test.info("Clicked Next — waiting for page 2 results.");

        List<String> page2Titles = home.getResultTitles();
        Assert.assertFalse(page2Titles.isEmpty(),
                "Expected non-empty results after clicking Next.");
        Assert.assertNotEquals(page2Titles.get(0), firstTitlePage1,
                "Page 2 should show different results from page 1.");
        test.pass("Page 2 loaded with different results.");

        // --- Click Previous ---
        home.clickPrev();
        home.waitForResultsToChange(page2Titles.get(0));
        test.info("Clicked Previous — waiting for page 1 results to return.");

        List<String> backToPage1Titles = home.getResultTitles();
        Assert.assertFalse(backToPage1Titles.isEmpty(),
                "Expected results after clicking Previous.");
        Assert.assertEquals(backToPage1Titles.get(0), firstTitlePage1,
                "Previous should restore page 1 first title.");
        test.pass("Previous returned to page 1 — first title matches.");

        // Write network log (informational only — no assertion)
        try {
            LogEntries perfLogs = driver.manage().logs().get("performance");
            perfLogs.forEach(e -> networkEvents.add(e.getMessage()));
            File logDir = new File("reports/networkLogs");
            logDir.mkdirs();
            File logFile = new File(logDir, "network_TC-P1.txt");
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
                networkEvents.forEach(pw::println);
            }
            test.info("Network log written: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            log.warning("Network log capture skipped: " + e.getMessage());
        }

        test.pass("Test passed.");
    }

    // -------------------------------------------------------------------------
    // TC-P2
    // -------------------------------------------------------------------------

    @Test(description = "TC-P2: Last page edge case. BUG-02 — last page returns error instead of results.")
    public void lastPageEdge() {
        startTest("TC-P2 - Last Page Edge Case");
        try {
            test.info("Opening home page and navigating to Popular category...");
            HomePage home = new HomePage(driver);
            home.open();
            home.clickCategory("Popular");
            test.info("Popular category loaded.");

            List<String> firstPageResults = home.getResultTitles();
            test.info("First page ready — results: " + firstPageResults.size());
            Assert.assertFalse(firstPageResults.isEmpty(), "Popular should have results on page 1.");

            test.info("Clicking last visible page number...");
            home.clickLastVisiblePage();
            test.info("Last page click executed.");

            boolean errorDisplayed = home.isPageError();
            int resultCount = home.getResultCount();
            String pageSource = driver.getPageSource();
            boolean somethingWrong = pageSource.contains("Something went wrong");

            test.info("Error message displayed: " + errorDisplayed);
            test.info("Result count on last page: " + resultCount);
            test.info("'Something went wrong' in page source: " + somethingWrong);

            if (errorDisplayed || somethingWrong || resultCount == 0) {
                test.warning("BUG-02: Last page shows error — 'Something went wrong! Please try again later.'");
                Assert.fail("BUG-02: Last page returns error instead of results.");
            } else {
                Assert.assertTrue(resultCount > 0, "Last page should return results.");
                test.pass("Last page returned " + resultCount + " results.");
            }

        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Unexpected exception: " + e.getMessage());
            log.severe("TC-P2 exception: " + e.getMessage());
            throw e;
        }
    }
}