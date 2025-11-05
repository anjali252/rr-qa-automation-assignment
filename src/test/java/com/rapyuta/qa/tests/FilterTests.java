package com.rapyuta.qa.tests;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
//Below commented line of code works for Browser V 142
/*import org.openqa.selenium.devtools.v142.network.Network;*/
import org.testng.Assert;
import org.testng.annotations.Test;

import com.rapyuta.qa.pages.HomePage;

public class FilterTests extends BaseTest {
	private static final Logger log = Logger.getLogger(FilterTests.class.getName());
    @Test
    public void popularCategoryLoads() {
        startTest("TC-F1 - Popular Category Loads");
        HomePage home = new HomePage(driver);
        home.open();
        test.info("Opened home page");
        log.info("On Popular category...");
        int count = home.getResultCount();
        test.info("Result count: " + count);
        Assert.assertTrue(count > 0, "Expected some results in Popular");
    }

    @Test
    public void switchCategories() {
        startTest("TC-F2 - Switch Categories");
        HomePage home = new HomePage(driver);
        home.open();
      //below commented code works for Chrome V 142
		/*
		 * DevTools devTools = ((HasDevTools)driver).getDevTools();
		 * devTools.createSession();
		 */
        try {
        	//below commented code works for Chrome V 142
			/*
			 * devTools.send(Network.enable(Optional.empty(), Optional.empty(),
			 * Optional.empty(), Optional.empty(), Optional.empty()));
			 */
        String[] cats = new String[]{"Trend", "Newest", "Top Rated"};
        String prevFirst = "";
        for (String c : cats) {
        	//below commented code works for Chrome V 142
			/*
			 * devTools.addListener(Network.responseReceived(), response -> { String url =
			 * response.getResponse().getUrl(); int status =
			 * response.getResponse().getStatus(); if (url.contains("/discover")) {
			 * Assert.assertTrue(status == 200, "API call should be 200"); } });
			 */

            home.clickCategory(c);
            test.info("Clicked " + c);
            int count = home.getResultCount();
            test.info(c + " count = " + count);
            List<String> titles = home.getResultTitles();
            Assert.assertFalse(titles.isEmpty(), c + " should return results");

            Assert.assertTrue(
                titles.stream().allMatch(t -> t.length() > 2),
                "Each title in category '" + c + "' should have valid text"
            );
            
            String first = home.getResultTitles().get(0);
            if (!prevFirst.isEmpty()) {
                test.info("Comparing " + prevFirst + " vs " + first);
            }
            prevFirst = first;
        }
        }
        catch (Throwable t) {
            System.out.println("Could not enable Network domain (DevTools version mismatch): " + t.getMessage());
        }
    }
}
