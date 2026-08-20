/* B110_ZK_5725_ToolbarAdjustTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 15:20:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Toolbar._adjustContent() postpones itself every 20ms while an image is still loading, so the
 * postponed run resumes long after the call that started it. It must not touch a toolbar that has
 * been unbound, or that has lost its overflow popup, in the meantime.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_ToolbarAdjustTest extends WebDriverTestCase {
	/** the page asks for an 8s image; give the answer a wide margin */
	private static final long LOADING_TIMEOUT_MS = 30000;
	/** the postponed run is 20ms away; 1s is fifty of those */
	private static final long SETTLE_MS = 1000;

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
		assertNoAnyError();
		assertEquals("false", getEval("zUtl.isImageLoading()"), "nothing should be loading yet");

		// (a) the toolbar is detached while a postponed _adjustContent() is still waiting
		click(jq("$loadDetach"));
		waitResponse();
		assertLoading("_adjustContent() only postpones itself while an image is loading");
		click(jq("$doDetach"));
		waitResponse();
		assertFalse(jq("$tbDetach").exists(), "the first toolbar should be gone");
		assertLoading("the toolbar must be detached before the image answers");
		awaitImageAnswered();
		assertNoPostponedError("detaching the toolbar");

		// (b) overflowPopup is turned off, so the redraw drops the popup and its button
		click(jq("$loadToggle"));
		waitResponse();
		assertLoading("_adjustContent() only postpones itself while an image is loading");
		click(jq("$doToggle"));
		waitResponse();
		assertTrue(jq("$tbToggle").exists(), "the second toolbar should still be there");
		assertFalse(jq("$tbToggle").find(".z-toolbar-overflowpopup-button").exists(),
				"turning overflowPopup off should drop the overflow popup button");
		assertLoading("overflowPopup must be turned off before the image answers");
		awaitImageAnswered();
		assertNoPostponedError("turning overflowPopup off");
	}

	/** The whole point of the page: without this, _adjustContent() never postpones anything. */
	private void assertLoading(String why) {
		assertEquals("true", getEval("zUtl.isImageLoading()"), why);
	}

	/** Waits until the slow image finally answers, which is what lets the postponed run resume. */
	private void awaitImageAnswered() {
		long deadline = System.currentTimeMillis() + LOADING_TIMEOUT_MS;
		while ("true".equals(getEval("zUtl.isImageLoading()"))) {
			if (System.currentTimeMillis() > deadline)
				fail("the slow image never answered");
			sleep(200);
		}
	}

	/**
	 * The throw happens in a timer callback, so the page collects it through window.onerror into
	 * window.zk5725Errors.
	 */
	private void assertNoPostponedError(String action) {
		sleep(SETTLE_MS);
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"the postponed _adjustContent() threw after " + action + ": "
						+ getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
