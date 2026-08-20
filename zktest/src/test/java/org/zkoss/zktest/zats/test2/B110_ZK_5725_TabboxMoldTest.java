/* B110_ZK_5725_TabboxMoldTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.ExternalZkXml;
import org.zkoss.test.webdriver.ForkJVMTestOnly;
import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The tablet override of Tabbox.setMold() only assigned the field, so the DOM kept the
 * mold it was drawn with. The next resize then looked for the accordion-only -cave2 node.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_TabboxMoldTest extends WebDriverTestCase {
	/** An Android tablet user agent, so that zk.mobile is true. */
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-X710)"
			+ " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	/** zktest disables the tablet UI by default, turn it back on. */
	@RegisterExtension
	public static final ExternalZkXml TABLET_UI = new ExternalZkXml("/test2/enable-tablet-ui-zk.xml");

	private static final int SETTLE_MS = 2000;

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options.setExperimentalOption("mobileEmulation", Map.of(
				"userAgent", USER_AGENT,
				"deviceMetrics", Map.of("width", 1024, "height", 768, "pixelRatio", 2, "touch", true)));
	}

	@Test
	public void test() {
		connect();
		waitResponse();
		assertEquals("true", getEval("!!zk.tabletUIEnabled"), "zk.tabletUIEnabled");
		assertEquals("0", getEval("jq('$tb [id$=\"-cave2\"]').length"),
				"the default mold has no -cave2 node");

		click(jq("$b1"));
		waitResponse();
		assertEquals("true", getEval("String(zk.Widget.$('$tb').inAccordionMold())"),
				"the tabbox should be in the accordion mold");

		// a throw during the resize pass leaves the AU cycle hanging, so settle by hand
		click(jq("$b2"));
		sleep(SETTLE_MS);
		failOnAlert("resizing the tabbox");
		assertFalse(hasError(), "resizing the tabbox failed: " + jq(".z-error").text());
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"resizing the tabbox threw: " + getEval("window.zk5725Errors.join(' | ')"));

		// ZK-5725: setMold() skipped rerender(), so the DOM stayed in the default mold
		assertEquals("2", getEval("jq('$tb [id$=\"-cave2\"]').length"),
				"switching to the accordion mold should redraw the tabpanels");
		assertNoJSError();
	}

	/**
	 * Fails with the text of the native alert the AU loop raises for a client side throw, if there
	 * is one. Such an alert also blocks every later WebDriver call, so read it before asserting
	 * anything else.
	 */
	private void failOnAlert(String what) {
		Alert alert;
		try {
			alert = getWebDriver().switchTo().alert();
		} catch (NoAlertPresentException e) {
			return;
		}
		String text = alert.getText();
		alert.dismiss();
		fail(what + " failed: " + text);
	}
}
