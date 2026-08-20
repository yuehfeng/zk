/* B110_ZK_5725_ToolbarStateTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 17:30:00 CST 2026, Created by peakerlee

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
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * A tabbox that does not scroll its tabs never draws its toolbar, so the toolbar buttons are bound
 * without a node. Changing their state from the server must not break the rest of the response.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_ToolbarStateTest extends WebDriverTestCase {

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
		// the whole point of the page: live widgets that were never drawn
		assertFalse(jq("$tb").exists(), "a tabbox with tabscroll=false draws no toolbar");
		assertEquals("true", getEval("String(!!zk.Widget.$('$b1').desktop)"),
				"the buttons are bound even though they are not drawn");

		// ZK-5725 threw "Node  is not found!" in the middle of the response
		click(jq("$disable"));
		awaitResponse("disabling a toolbarbutton that has no node");
		assertEquals("responses completed: 1", jq("$done").text(),
				"the rest of the response should still run");
		assertEquals("true", getEval("String(!!zk.Widget.$('$b1')._disabled)"),
				"the button should have taken the new state");

		click(jq("$check"));
		awaitResponse("checking a toolbarbutton that has no node");
		assertEquals("responses completed: 2", jq("$done").text(),
				"the rest of the response should still run");
		assertEquals("true", getEval("String(!!zk.Widget.$('$b2')._checked)"),
				"the button should have taken the new state");
		assertNoError("changing the state of buttons that have no node");

		// the control toolbar is drawn, so the same two commands really do change its DOM
		click(jq("$disableCtrl"));
		awaitResponse("disabling a drawn toolbarbutton");
		assertEquals("disabled", jq("$b1Ctrl").attr("disabled"),
				"the drawn button should be disabled");
		click(jq("$checkCtrl"));
		awaitResponse("checking a drawn toolbarbutton");
		assertTrue(jq("$b2Ctrl").hasClass("z-toolbarbutton-checked"),
				"the drawn toggle button should be checked");
		assertNoError("changing the state of drawn buttons");
	}

	/** A throw inside a response reaches the user as a native alert, not as an error box. */
	private void awaitResponse(String action) {
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail(action + " failed: " + e.getAlertText());
		}
		assertFalse(hasError(), action + " raised an error box");
	}

	private void assertNoError(String action) {
		assertEquals("0", getEval("window.zk5725Errors.length"),
				action + " threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
