package com.rapyuta.qa.tests;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.rapyuta.qa.pages.HomePage;

public class FilterTests extends BaseTest {

	private static final Logger log = Logger.getLogger(FilterTests.class.getName());

	@Test(description = "TC-F1: Verify the default 'Popular' category loads with visible results on page load.")
	public void popularCategoryLoads() {
		startTest("TC-F1 - Popular Category Loads");
		HomePage home = new HomePage(driver);
		home.open();
		test.info("Opened home page");
		log.info("Verifying Popular category loads with results...");
		int count = home.getResultCount();
		test.info("Result count: " + count);
		Assert.assertTrue(count > 0, "Expected some results in Popular category");
	}

	@Test(description = "TC-F2: Verify switching between Trending, Newest, and Top Rated categories returns unique results.")
	public void switchCategories() {
		startTest("TC-F2 - Switch Categories");
		HomePage home = new HomePage(driver);
		home.open();
		String[] cats = new String[] { "Trend", "Newest", "Top Rated" };
		for (String c : cats) {
			home.clickCategory(c);
			test.info("Clicked category: " + c);
			List<String> titles = home.getResultTitles();
			Assert.assertFalse(titles.isEmpty(), "Category '" + c + "' should return results");
			Assert.assertTrue(titles.stream().allMatch(t -> t.length() > 2),
					"Each title in '" + c + "' should have valid text");
			test.info(c + " returned " + titles.size() + " results.");
		}
	}

	@Test(description = "TC-F3: Verify filtering by Type (Movie / TV Show) returns non-empty results.")
	public void filterByType() {
	    startTest("TC-F3 - Filter by Type");
	    HomePage home = new HomePage(driver);
	    home.open();

	    // Exact text values shown in the React Select dropdown
	    String[] types = {"Movie", "TV"};
	    for (String type : types) {
	        home.open(); // reset page before each filter
	        log.info("Applying type filter: " + type);
	        home.selectType(type);
	        test.info("Selected type: " + type);

	        List<String> titles = home.getResultTitles();
	        test.info(type + " returned " + titles.size() + " results.");
	        Assert.assertFalse(titles.isEmpty(),
	            "Expected results when filtering by type: " + type);
	        test.pass("Type filter '" + type + "' returned " + titles.size() + " results.");
	    }
	}

	@Test(description = "TC-F4: Verify year range filter. BUG-03 — includes out-of-range results.")
	public void filterByYearRange() {
	    startTest("TC-F4 - Filter by Year Range");
	    try {
	        test.info("Opening home page...");
	        HomePage home = new HomePage(driver);
	        home.open();
	        test.info("Home page opened successfully.");

	        String fromYear = "2019";
	        String toYear   = "2021";
	        test.info("Applying year range: " + fromYear + " to " + toYear);
	        home.setYearRange(fromYear, toYear);
	        test.info("Year range applied.");

	        List<String> titles = home.getResultTitles();
	        test.info("Results returned: " + titles.size());
	        Assert.assertFalse(titles.isEmpty(),
	            "Expected results for year range " + fromYear + " - " + toYear);

	        List<String> years = home.getResultYears();
	        test.info("Years extracted from cards: " + years);

	        if (!years.isEmpty()) {
	            int from = Integer.parseInt(fromYear);
	            int to   = Integer.parseInt(toYear);
	            List<String> outOfRange = years.stream()
	                .filter(y -> !y.isEmpty())
	                .filter(y -> { int yr = Integer.parseInt(y); return yr < from || yr > to; })
	                .collect(java.util.stream.Collectors.toList());

	            if (!outOfRange.isEmpty()) {
	                test.warning("BUG-03: Out-of-range years detected: " + outOfRange);
	                Assert.fail("BUG-03: Year filter includes out-of-range results: " + outOfRange);
	            } else {
	                test.pass("All results within year range: " + fromYear + " - " + toYear);
	            }
	        } else {
	            test.warning("Year data not extractable — manual verification needed for BUG-03.");
	        }
	    } catch (AssertionError ae) {
	        test.fail("Assertion failed: " + ae.getMessage());
	        throw ae;
	    } catch (Exception e) {
	        test.fail("Unexpected exception: " + e.getMessage());
	        log.severe("TC-F4 failed with exception: " + e.getMessage());
	        throw e;
	    }
	}
	
	@Test(description = "TC-F5: Verify header search by title returns matching results.")
	public void searchByTitle() {
	    startTest("TC-F5 - Search by Title");
	    try {
	        test.info("Opening home page...");
	        HomePage home = new HomePage(driver);
	        home.open();
	        test.info("Home page opened.");

	        String searchTerm = "Batman";
	        test.info("Searching for title: " + searchTerm);

	        // Header search input
	        WebElement searchBox = new WebDriverWait(driver, Duration.ofSeconds(10))
	            .until(ExpectedConditions.visibilityOfElementLocated(
	                By.cssSelector("input[name='search']")));
	        searchBox.clear();
	        searchBox.sendKeys(searchTerm);
	        Thread.sleep(1500); // allow results to filter

	        List<String> titles = home.getResultTitles();
	        test.info("Results returned: " + titles.size());

	        Assert.assertFalse(titles.isEmpty(),
	            "Expected results for title search: " + searchTerm);

	        boolean anyMatch = titles.stream()
	            .anyMatch(t -> t.toLowerCase().contains(searchTerm.toLowerCase()));
	        Assert.assertTrue(anyMatch,
	            "At least one result should contain: " + searchTerm);

	        test.pass("Title search returned matching results.");
	    } catch (AssertionError ae) {
	        test.fail("Assertion failed: " + ae.getMessage());
	        throw ae;
	    } catch (Exception e) {
	        test.fail("Exception: " + e.getMessage());
	        throw new RuntimeException(e);
	    }
	}
	
	@Test(description = "TC-F6: Verify rating filter changes results. BUG-05 — results unchanged regardless of rating selected.")
	public void filterByRating() {
	    startTest("TC-F6 - Filter by Rating");
	    try {
	        test.info("Opening home page...");
	        HomePage home = new HomePage(driver);
	        home.open();
	        test.info("Home page opened.");

	        // Get baseline results with no rating filter
	        List<String> baselineTitles = home.getResultTitles();
	        test.info("Baseline results (no filter): " + baselineTitles.size());

	        // Apply 1 star rating
	        home.setRating(1);
	        test.info("Rating filter applied: 1 star & up");
	        Thread.sleep(1500);
	        List<String> oneStar = home.getResultTitles();
	        test.info("Results after 1 star: " + oneStar.size());

	        // Apply 5 star rating
	        home.setRating(5);
	        test.info("Rating filter applied: 5 stars & up");
	        Thread.sleep(1500);
	        List<String> fiveStar = home.getResultTitles();
	        test.info("Results after 5 stars: " + fiveStar.size());

	        // Results should differ between 1 star and 5 star
	        // If identical — rating filter is not working
	        if (oneStar.equals(fiveStar)) {
	            takeScreenshot("BUG05_RatingFilter_NoEffect");
	            test.warning("BUG-05: Rating filter has no effect — " 
	                + "1 star and 5 star return identical results: " + fiveStar);
	            Assert.fail("BUG-05: Rating filter does not change results. " 
	                + "1-star and 5-star filters return identical " 
	                + fiveStar.size() + " results.");
	        } else {
	            test.pass("Rating filter working — different results for different ratings.");
	        }

	    } catch (AssertionError ae) {
	        test.fail("Assertion failed: " + ae.getMessage());
	        throw ae;
	    } catch (Exception e) {
	        test.fail("Exception: " + e.getMessage());
	        throw new RuntimeException(e);
	    }
	}
	
	@Test(description = "TC-F7: Verify genre filter. BUG-04 — mixed genres returned.")
	public void filterByGenre() {
	    startTest("TC-F7 - Filter by Genre");
	    try {
	        test.info("Opening home page...");
	        HomePage home = new HomePage(driver);
	        home.open();
	        test.info("Home page opened successfully.");

	        String genre = "Action";
	        test.info("Selecting genre: " + genre);
	        home.selectGenre(genre);
	        test.info("Genre selected: " + genre);

	        List<String> titles = home.getResultTitles();
	        test.info("Results returned: " + titles.size());
	        Assert.assertFalse(titles.isEmpty(),
	            "Expected results when filtering by genre: " + genre);

	        List<String> cardGenres = home.getResultGenres();
	        test.info("Genres extracted from cards: " + cardGenres);

	        if (!cardGenres.isEmpty()) {
	            List<String> mismatches = cardGenres.stream()
	                .filter(g -> !g.toLowerCase().contains(genre.toLowerCase()))
	                .collect(java.util.stream.Collectors.toList());

	            if (!mismatches.isEmpty()) {
	                test.warning("BUG-04: Mixed genres detected: " + mismatches);
	                Assert.fail("BUG-04: Genre filter returned non-matching genres: " + mismatches);
	            } else {
	                test.pass("All results match genre: " + genre);
	            }
	        } else {
	            test.warning("Genre labels not extractable — skipping genre assertion.");
	        }
	    } catch (AssertionError ae) {
	        test.fail("Assertion failed: " + ae.getMessage());
	        throw ae;
	    } catch (Exception e) {
	        test.fail("Unexpected exception: " + e.getMessage());
	        log.severe("TC-F7 failed with exception: " + e.getMessage());
	        throw e;
	    }
	}
	
	@Test(description = "TC-F8: Verify direct URL navigation. BUG-01 — Surge.sh 404 shown on direct access.")
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

	            boolean hasResults  = home.getResultCount() > 0;
	            boolean hasError    = home.isPageError();
	            String  pageSource  = driver.getPageSource().toLowerCase();
	            boolean surge404    = pageSource.contains("page not found")
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
	        log.severe("TC-F8 failed with exception: " + e.getMessage());
	        throw e;
	    }
	}
}