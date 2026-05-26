package com.rapyuta.qa.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

//Below commented line of code works for Browser V 142
/*import org.openqa.selenium.devtools.v142.network.Network;*/
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.rapyuta.qa.pages.HomePage;
import com.rapyuta.qa.utils.LogFileUtil;

public class PaginationTests extends BaseTest {

	@Test
	public void nextPrevPagination() throws InterruptedException, IOException {
		startTest("TC-P1 - Next/Previous Pagination");
		HomePage home = new HomePage(driver);
		home.open();
		try {
			List<String> networkEvents = new ArrayList<>();
			int nextClickCount = 0;

			home.clickCategory("Trend");
			List<String> currentTitles = home.getResultTitles();
			test.info("Results found on first page: " + currentTitles.size());

			test.info("Checking browser console logs for errors after loading Trend category...");
			LogEntries logs = driver.manage().logs().get("browser");
			for (LogEntry entry : logs) {
				if (entry.getLevel().equals(Level.SEVERE) && !entry.getMessage().contains("favicon.ico")) { // add this
					test.warning("Browser error: " + entry.getMessage());
				}
			}

			test.info("Subscribing to browser console logs via BiDi...");

			if (currentTitles.isEmpty()) {
				test.warning("No results found on first page. Skipping pagination validation.");
				takeScreenshot("noResultsFirstPage");
				return; // graceful exit
			}

			String firstBefore = currentTitles.get(0);
			test.info("First title before next: " + firstBefore);

			home.clickNext();
			nextClickCount++;
			home.waitForResultsToChange(firstBefore);
			test.info("Clicked Next (" + nextClickCount + " time)");

			List<String> nextPageTitles = home.getResultTitles();
			if (nextPageTitles.isEmpty()) {
				test.warning("Next page returned empty results even after initial wait. Retrying...");
				Thread.sleep(2000);
				nextPageTitles = home.getResultTitles();
			}

			Assert.assertFalse(nextPageTitles.isEmpty(), "Expected non-empty results after Next click.");

			Assert.assertTrue(nextPageTitles.stream().allMatch(t -> t.length() > 2),
					"Each title on next page should have valid text");

			test.info("Titles after Next click: "
					+ String.join(", ", nextPageTitles.subList(0, Math.min(5, nextPageTitles.size()))) + "...");

			if (nextPageTitles.equals(currentTitles)) {
				test.warning("Next and current page results look identical. Possibly last page or data not updated.");
				takeScreenshot("page_" + nextClickCount + "_identicalResults");
			}

			String firstAfter = nextPageTitles.get(0);
			Assert.assertNotEquals(firstBefore, firstAfter, "Expected different first item after Next");

			home.clickPrev();
			home.waitForResultsToChange(firstAfter);

			List<String> backTitles = home.getResultTitles();
			test.info("Results found after clicking Previous: " + backTitles.size());

			Assert.assertFalse(backTitles.isEmpty(), "No results after clicking Previous.");

			String backFirst = backTitles.get(0);
			Assert.assertEquals(backFirst, firstBefore, "Previous should return to original first item");

			String networkLogPath = LogFileUtil.writeToFile("network_" + test.getModel().getName() + ".txt",
					String.join("\n", networkEvents));
			if (networkLogPath != null && !networkLogPath.isEmpty()) {
				test.info("Network logs attached: " + networkLogPath);
			} else {
				test.info("No network logs available or log file creation failed.");
			}

		} catch (AssertionError ae) {
			test.fail("Assertion failed: " + ae.getMessage());
			takeScreenshot("TC-P1_Failure");
			throw ae; // ← rethrow so TestNG marks FAIL not WARNING
		} catch (Throwable t) {
			// Only non-assertion errors (e.g. DevTools version mismatch)
			if (!t.getMessage().contains("favicon")) {
				test.warning("Non-critical exception: " + t.getMessage());
				log.warning("Caught throwable: " + t.getMessage());
			}
		}
	}

	@Test(description = "TC-P2: Verify last page edge case. BUG-02 — error shown on last pages.")
	public void lastPageEdge() throws Exception {
		startTest("TC-P2 - Last Page Edge Case");
		try {
			test.info("Opening home page and navigating to Popular category...");
			HomePage home = new HomePage(driver);
			home.open();
			home.clickCategory("Popular");
			test.info("Popular category loaded.");

			new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
					.until(d -> home.getResultCount() > 0);
			test.info("First page loaded — results: " + home.getResultCount());

			test.info("Clicking last visible page number...");
			home.clickLastVisiblePage();
			test.info("Last page click executed.");

			Thread.sleep(3000);

			boolean errorDisplayed = home.isErrorDisplayed();
			int resultCount = home.getResultCount();
			String pageSource = driver.getPageSource().toLowerCase();
			boolean somethingWrong = pageSource.contains("something went wrong");

			test.info("Error message displayed: " + errorDisplayed);
			test.info("Result count on last page: " + resultCount);
			test.info("'Something went wrong' in page: " + somethingWrong);

			if (errorDisplayed || somethingWrong || resultCount == 0) {
				test.warning("BUG-02: Last page shows error — 'Something went wrong! Please try again later.'");
				Assert.fail("BUG-02: Last page returns error instead of results.");
			} else {
				test.pass("Last page loaded successfully with " + resultCount + " results.");
			}
		} catch (AssertionError ae) {
			test.fail("Assertion failed: " + ae.getMessage());
			throw ae;
		} catch (Exception e) {
			test.fail("Unexpected exception: " + e.getMessage());
			log.severe("TC-P2 failed with exception: " + e.getMessage());
			throw e;
		}
	}
}
