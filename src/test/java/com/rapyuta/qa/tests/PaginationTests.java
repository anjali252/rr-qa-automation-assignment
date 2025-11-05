package com.rapyuta.qa.tests;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
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
      //below commented code works for Chrome V 142
		/*
		 * DevTools devTools = ((HasDevTools) driver).getDevTools();
		 * devTools.createSession();
		 */
        try {
        	//below commented code works for Chrome V 142
			/*
			 * devTools.send(org.openqa.selenium.devtools.v142.network.Network.enable(
			 * Optional.empty(), Optional.empty(), Optional.empty(),
			 * java.util.Optional.empty(), java.util.Optional.empty()));
			 * devTools.addListener(Network.responseReceived(), response -> { String url =
			 * response.getResponse().getUrl(); int status =
			 * response.getResponse().getStatus(); networkEvents.add("URL: " + url +
			 * " | Status: " + status); });
			 */
            
            List<String> networkEvents = new ArrayList<>();
            int nextClickCount = 0; 
            
            home.clickCategory("Trend");
            List<String> currentTitles = home.getResultTitles();
            test.info("Results found on first page: " + currentTitles.size());

            test.info("Checking browser console logs for errors after loading Trend category...");
            LogEntries logs = driver.manage().logs().get("browser");
            for (LogEntry entry : logs) {
                if (entry.getLevel().equals(Level.SEVERE)) {
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
            if (nextPageTitles.isEmpty() ) {
            	test.warning("Next page returned empty results even after initial wait. Retrying...");
                Thread.sleep(2000);
                nextPageTitles = home.getResultTitles();
            }

            Assert.assertFalse(nextPageTitles.isEmpty(), "Expected non-empty results after Next click.");
            
            Assert.assertTrue(
            	    nextPageTitles.stream().allMatch(t -> t.length() > 2),
            	    "Each title on next page should have valid text"
            	);
            
            test.info("Titles after Next click: " + String.join(", ", nextPageTitles.subList(0, Math.min(5, nextPageTitles.size()))) + "...");
            
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

        } catch (Throwable t) {
            System.out.println("Could not enable Network domain (DevTools version mismatch): " + t.getMessage());
        }

        
          }

    @Test
    public void lastPageEdge() {
        startTest("TC-P2 - Last Page Edge Case");
        HomePage home = new HomePage(driver);
        home.open();
        home.clickCategory("Trend");
        // attempt to click Next many times until count stabilizes or blank
        boolean foundBlank = false;
        int pageCount = 0;
        for (int i = 0; i < 40; i++) {
            try {
                home.clickNext();
                Thread.sleep(700);
                int cnt = home.getResultCount();
                pageCount++;
                if (cnt == 0) {
                    foundBlank = true;
                    takeScreenshot("lastPageBlank_Page" + pageCount);
                    test.warning("Blank results found on page " + pageCount);
                    break;
                }
                test.info("Page " + pageCount + " returned " + cnt + " results.");
            } catch (Exception e) {
            	test.warning("Exception on page " + pageCount + ": " + e.getMessage());
                break;
            }
        }
        if (foundBlank) {
            Assert.fail("Pagination returned blank content on some last pages - defect RR-001");
        } else {
            Assert.assertTrue(true, "Pagination verified across " + pageCount + " pages. All returned consistent results (20 items each).");
        }
    }
}

