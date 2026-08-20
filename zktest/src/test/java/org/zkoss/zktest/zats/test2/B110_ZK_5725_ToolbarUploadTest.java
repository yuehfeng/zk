/* B110_ZK_5725_ToolbarUploadTest.java

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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * An upload builds its file input right next to the button's node, so a button that is bound
 * without a node must not build one - neither at bind time nor when it is re-enabled.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_ToolbarUploadTest extends WebDriverTestCase {

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

		// the setup: a tabbox with tabscroll=false draws no toolbar, so both buttons are live
		// widgets without a node, and both of them carry an upload
		assertFalse(jq("$tb").exists(), "a tabbox with tabscroll=false draws no toolbar");
		for (String id : new String[] { "$tbbUpload", "$btnUpload" }) {
			assertEquals("true", getEval("String(!!zk.Widget.$('" + id + "').desktop)"),
					id + " should be bound even though it is not drawn");
			assertEquals("undefined", getEval("String(zk.Widget.$('" + id + "').$n())"),
					id + " should have no node");
			assertEquals("true", getEval("String(!!zk.Widget.$('" + id + "')._upload)"),
					id + " should carry an upload");
		}

		// zul.Upload.initContent() dereferences wgt.$n(), so building one at bind time threw
		// "Cannot read properties of undefined (reading 'offsetHeight')" during the mount
		assertNoError("mounting the page");
		for (String id : new String[] { "$tbbUpload", "$btnUpload" }) {
			assertEquals("undefined", getEval("String(zk.Widget.$('" + id + "')._uplder)"),
					id + " must not build an uploader while it has no node");
		}

		// disabling drops the uploader, re-enabling builds it again - both must stay away from
		// the missing node instead of throwing in the middle of the response
		click(jq("$disable"));
		awaitResponse("disabling two buttons that have no node");
		assertEquals("responses completed: 1", jq("$done").text(),
				"the rest of the response should still run");

		click(jq("$enable"));
		awaitResponse("re-enabling two buttons that have no node");
		assertEquals("responses completed: 2", jq("$done").text(),
				"the rest of the response should still run");
		for (String id : new String[] { "$tbbUpload", "$btnUpload" }) {
			assertEquals("undefined", getEval("String(zk.Widget.$('" + id + "')._uplder)"),
					id + " must not build an uploader when it is re-enabled without a node");
		}
		assertNoError("changing the disabled state of buttons that have no node");

		// the control toolbar is drawn, so the very same upload really does build a file input
		for (String id : new String[] { "$tbbCtrl", "$btnCtrl" }) {
			assertEquals("true", getEval("String(!!zk.Widget.$('" + id + "')._uplder)"),
					id + " should build an uploader, it is drawn");
		}
		assertTrue(jq(".z-upload input").length() >= 2,
				"the drawn buttons should carry a file input each");
		assertNoError("the drawn control buttons");
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
