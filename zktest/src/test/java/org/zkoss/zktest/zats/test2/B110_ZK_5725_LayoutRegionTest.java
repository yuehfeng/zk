/* B110_ZK_5725_LayoutRegionTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 16:00:00 CST 2026, Created by peakerlee

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
 * A layout region drops its "nested" style class when a nested borderlayout is removed, which it
 * cannot do when the region itself is not drawn.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_LayoutRegionTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	@Test
	public void test() {
		connect();
		// the region is bound but has no DOM node, which is what onChildRemoved_() has to survive
		assertEquals("true", getEval("(window.__c = zk.Widget.$('$c'), !!window.__c)"),
				"the render-deferred center should still be a bound widget");
		assertEquals("false", getEval("String(!!window.__c.$n())"),
				"the render-deferred center should have no DOM node");
		assertEquals("1", getEval("String(window.__c.nChildren)"),
				"the center should hold the nested borderlayout");

		click(jq("$detach"));
		// ZK-5725 threw "Node  is not found!" while processing the rm command, which aborts the
		// whole AU response and pops a native retry alert
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("the AU command failed: " + e.getAlertText());
		}
		assertFalse(hasError(), "the AU command failed: " + jq(".z-error").text());
		assertEquals("0", getEval("String(window.__c.nChildren)"),
				"the nested borderlayout should be removed from the center");
		assertNoJSError();
	}
}
