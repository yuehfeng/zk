/* B110_ZK_5725_CardlayoutRodDesktopTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The desktop half of {@link B110_ZK_5725_CardlayoutRodTest}: the base Cardlayout asserts
 * its nodes non-null instead of throwing, so the same removal dies on a TypeError there.
 * Fixing only the tablet fork would just change the message.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_CardlayoutRodDesktopTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	@Test
	public void test() {
		connect("/test2/B110-ZK-5725-CardlayoutRod.zul");
		waitResponse();
		assertEquals("false", getEval("!!zk.tabletUIEnabled"), "zk.tabletUIEnabled");

		click(jq("$rm"));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("removing a card failed: " + e.getAlertText());
		}
		assertFalse(hasError(), "removing a card failed: " + jq(".z-error").text());
		assertEquals("removed", jq("$done").text(), "the rest of the response should still apply");
		assertEquals("2", getEval("String(zk.Widget.$('$cl').nChildren)"), "the card should be gone");
		assertNoJSError();
	}
}
