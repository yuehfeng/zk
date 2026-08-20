/* B110_ZK_5725_NodelessUnbindTest.java

	Purpose:

	Description:

	History:
		Mon Aug 25 10:40:00 CST 2026, Created by peakerlee

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
 * A toolbar its parent never draws leaves its children bound without a node. Asking such a button
 * for the focus, and taking it away again, both used to dereference that node.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_NodelessUnbindTest extends WebDriverTestCase {

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
		assertEquals("true", getEval("String(!!zk.Widget.$('$tbb').desktop)"),
				"the toolbarbutton is bound even though it is not drawn");
		assertEquals("true", getEval("String(!!zk.Widget.$('$btn').desktop)"),
				"the button is bound even though it is not drawn");

		// ZK-5725 threw "Node  is not found!" out of Toolbarbutton.focus_
		click(jq("$focusIt"));
		awaitResponse("focusing a toolbarbutton that has no node");
		assertEquals("responses completed: 1", jq("$done").text(),
				"the rest of the response should still run");

		// ZK-5725 threw the same out of Toolbarbutton.unbind_ and Button.unbind_
		click(jq("$detachIt"));
		awaitResponse("detaching a tabbox whose toolbar was never drawn");
		assertEquals("responses completed: 2", jq("$done").text(),
				"the rest of the response should still run");
		assertFalse(jq("$tbx").exists(), "the tabbox should be gone");
		assertEquals("false", getEval("String(!!zk.Widget.$('$tbb'))"),
				"its buttons should have unbound with it");
		assertNoError("focusing and unbinding buttons that have no node");

		// the control toolbar is drawn, so the same detach really does take a real toolbar away
		click(jq("$detachCtrl"));
		awaitResponse("detaching a drawn tabbox");
		assertFalse(jq("$tbxCtrl").exists(), "the control tabbox should be gone");
		assertFalse(jq("$tbbCtrl").exists(), "its drawn button should be gone too");
		assertNoError("detaching a drawn tabbox");
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
