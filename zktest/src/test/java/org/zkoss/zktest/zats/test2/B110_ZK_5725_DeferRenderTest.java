/* B110_ZK_5725_DeferRenderTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 16:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The render-defer timer must give up when the placeholder it is supposed to replace is already
 * gone, instead of binding a widget that has no DOM.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_DeferRenderTest extends WebDriverTestCase {

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
		assertEquals("true", getEval("(window.__d = zk.Widget.$('$d'), !!window.__d._z$rd)"),
				"the div should still be waiting for its render-defer timer");

		click(jq("$detach"));
		waitResponse();
		// without this the detach could happen after the timer already fired, and the test would
		// pass for the wrong reason
		assertEquals("true", getEval("String(!!window.__d._z$rd)"),
				"the detach must happen before the render-defer timer fires");

		// the control proves the timers really fired by then
		sleep(6000);
		assertTrue(jq("$sbCtrl").exists(), "the control selectbox should be rendered by now");

		// ZK-5725 threw "Node  is not found!" from the timer of the detached div
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"the render-defer timer threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
