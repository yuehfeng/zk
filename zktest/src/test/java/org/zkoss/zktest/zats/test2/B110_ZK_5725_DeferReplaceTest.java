/* B110_ZK_5725_DeferReplaceTest.java

	Purpose:

	Description:

	History:
		Fri Aug 21 10:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Invalidating an ancestor gives the uuid of a render-deferred widget to a brand new widget, so
 * the pending timer of the old one must not claim the node that now belongs to its replacement.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_DeferReplaceTest extends WebDriverTestCase {

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
		assertEquals("true", getEval("(window.__old = zk.Widget.$('$d'), String(!!window.__old._z$rd))"),
				"the div should still be waiting for its render-defer timer");
		getEval("(window.__uuid = window.__old.uuid, '')");

		click(jq("$go"));
		waitResponse();

		// the setup: a different widget object now answers to the same uuid
		assertEquals("false", getEval("String(zk.Widget.$('$d') === window.__old)"),
				"invalidating the parent should build a new widget");
		assertEquals("true", getEval("String(zk.Widget.$('$d').uuid === window.__uuid)"),
				"the new widget should reuse the uuid of the widget it replaced");
		assertEquals("true", getEval("String(document.getElementById(window.__uuid).className === 'z-renderdefer')"),
				"the new widget should have drawn its own placeholder");
		// without this the invalidation could land after the old timer already fired, and the test
		// would pass for the wrong reason
		assertEquals("true", getEval("String(!!window.__old._z$rd)"),
				"the invalidation must happen before the render-defer timer of the old widget fires");

		// past the 5s the old widget asked for, and far from the 600s its replacement asked for
		sleep(7000);

		// the control proves the timers really fired by then
		assertTrue(jq("$innerCtrl").exists(), "the control should be rendered by now");

		// ZK-5725: the old timer only checked that some node carries the uuid, so it drew the old
		// widget over the placeholder of its replacement and took the node away from its owner
		assertEquals("true", getEval("String(!!window.__old._z$rd)"),
				"the timer of the replaced widget must give up instead of rendering");
		assertFalse(jq("$inner").exists(),
				"the replaced widget must not draw itself into the node of its replacement");
		assertEquals("true", getEval("String(document.getElementById(window.__uuid).className === 'z-renderdefer')"),
				"the placeholder of the new widget should be untouched");
		assertEquals("true", getEval("String(zk.Widget.getWidgetByUuid(window.__uuid) === zk.Widget.$('$d'))"),
				"the node should still belong to the widget that drew it");

		assertEquals("0", getEval("window.zk5725Errors.length"),
				"the render-defer timer threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
