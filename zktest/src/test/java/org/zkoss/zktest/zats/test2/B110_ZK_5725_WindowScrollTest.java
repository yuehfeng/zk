/* B110_ZK_5725_WindowScrollTest.java

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
 * The tablet mold of Window builds its -cave2 node only after the whole bind chain of the
 * children ran, and a flex child asks the window for that node meanwhile. The miss is
 * cached, so -cave2 stays unreachable for the rest of the life of the window.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_WindowScrollTest extends WebDriverTestCase {
	/** An Android tablet user agent, so that zk.mobile is true. */
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-X710)"
			+ " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	/** zktest disables the tablet UI by default, turn it back on. */
	@RegisterExtension
	public static final ExternalZkXml TABLET_UI = new ExternalZkXml("/test2/enable-tablet-ui-zk.xml");

	private static final int SETTLE_MS = 3000;


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

		// a throw while binding the window leaves the AU cycle hanging, so settle by hand
		click(jq("$add"));
		sleep(SETTLE_MS);
		failOnAlert("adding the window");
		assertFalse(hasError(), "adding the window failed: " + jq(".z-error").text());
		assertEquals("false", getEval("String(zk.Widget.$('$w')._nativebar)"), "window._nativebar");

		// the -cave2 node does exist in the DOM ...
		assertEquals("1", getEval("jq('$w [id$=\"-cave2\"]').length"),
				"the tablet mold of Window should wrap its content in a -cave2 node");
		// ... but ZK-5725: the lookup that ran before it existed cached the miss forever
		assertNotEquals("n/a", getEval("String(zk.Widget.$('$w')._subnodes['cave2'])"),
				"the -cave2 lookup was cached as a miss before the node was created");

		click(jq("$rz"));
		sleep(SETTLE_MS);
		failOnAlert("resizing the window");
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"resizing the window threw: " + getEval("window.zk5725Errors.join(' | ')"));
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
