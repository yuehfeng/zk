/* B110_ZK_5725_AuxheadTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 17:30:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Hiding a header re-measures the body against the header row in a timer, and a grid that has an
 * auxhead but no columns draws no header row at all.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_AuxheadTest extends WebDriverTestCase {
	/** the re-measuring timer is 0ms away; half a second is a wide margin */
	private static final long SETTLE_MS = 500;

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
		waitResponse();
		assertNoError("mounting the page");
		// the whole point of the page: this grid has no header row to measure
		assertEquals(0, jq("$g").find(".z-grid-header").length(),
				"a grid without columns draws no header row");
		assertEquals("true", getEval("String(!!zk.Widget.$('$ah').desktop)"),
				"the auxhead is bound even though it is not drawn");

		click(jq("$hide"));
		waitResponse();
		sleep(SETTLE_MS);
		assertFalse(getEval("String(zk.Widget.$('$ah').isVisible())").equals("true"),
				"the auxhead should be hidden");
		// ZK-5725 threw "Node with head is not found!" out of the re-measuring timer
		assertNoError("hiding the auxhead");

		// the control has columns, so its auxhead row really is drawn and really goes away
		assertEquals("1", visibleControlAuxheads(), "the control auxhead should be drawn");
		click(jq("$hideCtrl"));
		waitResponse();
		sleep(SETTLE_MS);
		assertEquals("0", visibleControlAuxheads(), "the control auxhead should be hidden");
		assertNoError("hiding the control auxhead");
	}

	private String visibleControlAuxheads() {
		return getEval("String(jq('$gCtrl').find('.z-auxhead:visible').length)");
	}

	/** The throw happens in a timer callback, so the page collects it through window.onerror. */
	private void assertNoError(String action) {
		assertEquals("0", getEval("window.zk5725Errors.length"),
				action + " threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
