package com.rapyuta.qa.tests;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rapyuta.qa.pages.HomePage;

/**
 * FilterTests — TC-F1 through TC-F8.
 *  */
public class FilterTests extends BaseTest {

    private static final Logger log = Logger.getLogger(FilterTests.class.getName());

    // -------------------------------------------------------------------------
    // TC-F1
    // -------------------------------------------------------------------------

    @Test(description = "TC-F1: Verify the default 'Popular' category loads with visible results on page load.")
    public void popularCategoryLoads() {
        startTest("TC-F1 - Popular Category Loads");
        HomePage home = new HomePage(driver);
        home.open();
        test.info("Opened home page.");

        int count = home.getResultCount();
        test.info("Result count on load: " + count);

        Assert.assertTrue(count > 0,
                "Expected results in Popular category on page load, found: " + count);
        test.pass("Popular category loaded with " + count + " results.");
    }

    // -------------------------------------------------------------------------
    // TC-F2
    // -------------------------------------------------------------------------

    @Test(description = "TC-F2: Verify switching between Trending, Newest, and Top Rated returns valid results.")
    public void switchCategories() {
        startTest("TC-F2 - Switch Categories");
        HomePage home = new HomePage(driver);
        home.open();
        test.info("Home page opened.");

        String[] categories = {"Trend", "Newest", "Top rated"};

        for (String category : categories) {
            home.clickCategory(category);
            test.info("Clicked category: " + category);

            List<String> titles = home.getResultTitles();
            Assert.assertFalse(titles.isEmpty(),
                    "Category '" + category + "' should return results.");
            Assert.assertTrue(titles.stream().allMatch(t -> t.length() > 2),
                    "All titles in '" + category + "' should have valid text.");
            test.pass(category + " returned " + titles.size() + " valid results.");
        }
    }

    // -------------------------------------------------------------------------
    // TC-F3
    // -------------------------------------------------------------------------

    @Test(description = "TC-F3: Verify filtering by Type (Movie / TV) returns non-empty results.")
    public void filterByType() {
        startTest("TC-F3 - Filter by Type");
        String[] types = {"Movie", "TV"};

        for (String type : types) {
            HomePage home = new HomePage(driver);
            home.open();
            test.info("Home page opened for type: " + type);

            home.selectType(type);
            test.info("Selected type: " + type);

            List<String> titles = home.getResultTitles();
            test.info(type + " returned " + titles.size() + " results.");

            Assert.assertFalse(titles.isEmpty(),
                    "Expected results for type filter: " + type);
            test.pass("Type filter '" + type + "' returned " + titles.size() + " results.");
        }
    }

    // -------------------------------------------------------------------------
    // TC-F4
    // -------------------------------------------------------------------------

    @Test(description = "TC-F4: Verify year range filter. BUG-03 — includes out-of-range results.")
    public void filterByYearRange() {
        startTest("TC-F4 - Filter by Year Range");
        try {
            test.info("Opening home page...");
            HomePage home = new HomePage(driver);
            home.open();
            test.info("Home page ready.");

            String fromYear = "2019";
            String toYear   = "2021";
            test.info("Applying year range: " + fromYear + " to " + toYear);
            home.setYearRange(fromYear, toYear);
            test.info("Year range applied.");

            List<String> rawYears = home.getResultYears();
            test.info("Years on cards: " + rawYears);

            List<Integer> years = rawYears.stream()
                    .map(String::trim)
                    .filter(s -> s.matches("\\d{4}"))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            List<Integer> outOfRange = years.stream()
                    .filter(y -> y < 2019 || y > 2021)
                    .collect(Collectors.toList());

            if (!outOfRange.isEmpty()) {
                test.warning("BUG-03: Out-of-range years detected: " + outOfRange);
                Assert.fail("BUG-03: Year filter includes out-of-range results: " + outOfRange);
            } else {
                test.pass("All " + years.size() + " results are within 2019–2021.");
            }
        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // TC-F5
    // -------------------------------------------------------------------------

    @Test(description = "TC-F5: Verify title search returns matching results.")
    public void searchByTitle() {
        startTest("TC-F5 - Search by Title");
        try {
            test.info("Opening home page...");
            HomePage home = new HomePage(driver);
            home.open();
            test.info("Home page opened.");

            String searchTerm = "Batman";
            test.info("Searching for: " + searchTerm);

            home.searchByTitle(searchTerm);

            List<String> titles = home.getResultTitles();
            test.info("Results returned: " + titles.size());

            Assert.assertFalse(titles.isEmpty(),
                    "Expected results for title search: " + searchTerm);

            boolean anyMatch = titles.stream()
                    .anyMatch(t -> t.toLowerCase().contains(searchTerm.toLowerCase()));
            Assert.assertTrue(anyMatch,
                    "At least one result should contain: " + searchTerm);

            test.pass("Title search for '" + searchTerm + "' returned matching results.");
        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // TC-F6
    // -------------------------------------------------------------------------

    @Test(description = "TC-F6: Verify rating filter changes result cards.")
    public void filterByRating() {
        startTest("TC-F6 - Filter by Rating");
        try {
            test.info("Opening home page...");
            HomePage home = new HomePage(driver);
            home.open();
            test.info("Home page opened.");

            List<String> baseline = home.getResultTitles();
            test.info("Baseline results (no filter): " + baseline.size());

            home.setRating(1);
            test.info("Rating filter applied: 1 star & up");
            List<String> oneStar = home.getResultTitles();
            test.info("Results after 1 star: " + oneStar.size());

            Assert.assertFalse(oneStar.isEmpty(),
                    "Expected results after applying 1-star rating filter.");

            home.setRating(5);
            test.info("Rating filter applied: 5 stars & up");
            List<String> fiveStar = home.getResultTitles();
            test.info("Results after 5 stars: " + fiveStar.size());

            Assert.assertFalse(fiveStar.isEmpty(),
                    "Expected results after applying 5-star rating filter.");

            // A stricter threshold should return fewer or equal results, not more.
            Assert.assertTrue(fiveStar.size() <= oneStar.size(),
                    "5-star filter should return fewer or equal results than 1-star filter. "
                    + "Got: 1-star=" + oneStar.size() + ", 5-star=" + fiveStar.size());

            // The two result sets should differ (rating filter has a real effect).
            Assert.assertNotEquals(oneStar, fiveStar,
                    "1-star and 5-star filters should return different result sets.");

            test.pass("Rating filter working — 1-star returned " + oneStar.size()
                    + " results, 5-star returned " + fiveStar.size() + " results.");
        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // TC-F7
    // -------------------------------------------------------------------------

    @Test(description = "TC-F7: Verify genre filter. BUG-04 — mixed genres returned.")
    public void filterByGenre() {
        startTest("TC-F7 - Filter by Genre");
        try {
            test.info("Opening home page...");
            HomePage home = new HomePage(driver);
            home.open();
            test.info("Home page opened.");

            String genre = "Action";
            test.info("Selecting genre: " + genre);
            home.selectGenre(genre);
            test.info("Genre selected.");

            int count = home.getResultCount();
            test.info("Results returned: " + count);

            List<String> genres = home.getResultGenres();
            test.info("Genres on cards: " + genres);

            List<String> nonMatching = genres.stream()
                    .filter(g -> !g.equalsIgnoreCase(genre))
                    .distinct()
                    .collect(Collectors.toList());

            if (!nonMatching.isEmpty()) {
                takeScreenshot("BUG04_GenreFilter_MixedResults");
                test.warning("BUG-04: Mixed genres detected: " + nonMatching);
                Assert.fail("BUG-04: Genre filter returned non-matching genres: " + nonMatching);
            } else {
                test.pass("Genre filter working — all " + count + " results are Action.");
            }
        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // TC-F8
    // -------------------------------------------------------------------------

    @Test(description = "TC-F8: Verify direct URL navigation. BUG-01 — Surge.sh 404 on direct access.")
    public void directUrlNavigation() {
        startTest("TC-F8 - Direct URL Navigation");
        try {
            test.info("Testing direct URL navigation for known broken routes...");
            HomePage home = new HomePage(driver);

            String[] brokenRoutes = {
                "https://tmdb-discover.surge.sh/popular",
                "https://tmdb-discover.surge.sh/top"
            };

            for (String url : brokenRoutes) {
                test.info("Navigating to: " + url);
                driver.get(url);

                new org.openqa.selenium.support.ui.WebDriverWait(driver,
                        java.time.Duration.ofSeconds(8))
                        .until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
                                .executeScript("return document.readyState").equals("complete"));

                boolean hasResults = home.getResultCount() > 0;
                boolean hasError   = home.isPageError();
                String  pageSource = driver.getPageSource().toLowerCase();
                boolean surge404   = pageSource.contains("page not found")
                                  || pageSource.contains("powered by")
                                  || pageSource.contains("surge.sh");

                test.info("Results visible: " + hasResults);
                test.info("App error detected: " + hasError);
                test.info("Surge 404 detected: " + surge404);

                if (!hasResults || hasError || surge404) {
                    test.warning("BUG-01: '" + url + "' shows 404 instead of app content.");
                    Assert.fail("BUG-01: Direct URL '" + url + "' not working — 404 page displayed.");
                } else {
                    test.pass("Direct navigation to " + url + " loaded successfully.");
                }
            }
        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Unexpected exception: " + e.getMessage());
            log.severe("TC-F8 exception: " + e.getMessage());
            throw e;
        }
    }
}