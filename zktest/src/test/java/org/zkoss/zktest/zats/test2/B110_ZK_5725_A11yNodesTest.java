/* B110_ZK_5725_A11yNodesTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 21:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The za11y overrides of getA11yRealNode_() and focus_() have to keep the tolerant contract of
 * their base, and Radiogroup must survive a ghost extern radio left behind by an un-rod.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_A11yNodesTest extends WebDriverTestCase {

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
		// everything under test is added by the za11y layer. A silent return would make the NO_A11Y run
		// report a pass, so abort the test instead and let it show up as skipped.
		Assumptions.assumeTrue(Boolean.parseBoolean(getEval("!!window.za11y")),
				"the za11y layer is not loaded, nothing under test exists");

		// the setup: the deferred widgets are bound but have no DOM node
		assertEquals("true", getEval("String(!!zk.Widget.$('$cbDefault'))"),
				"the deferred checkbox should be a bound widget");
		assertEquals("false", getEval("String(!!zk.Widget.$('$cbDefault').desktop)"),
				"the deferred checkbox should have no DOM node yet");

		// ZK-5725: the three getA11yRealNode_ overrides threw instead of answering null
		assertEquals("undefined", call("$cbDefault", "getA11yRealNode_()"),
				"Checkbox#getA11yRealNode_ must tolerate a missing -real node");
		assertEquals("undefined", call("$cbSwitch", "getA11yRealNode_()"),
				"Checkbox#getA11yRealNode_ must tolerate a missing -mold node");
		assertEquals("undefined", call("$im", "getA11yRealNode_()"),
				"Imagemap#getA11yRealNode_ must tolerate a missing -real node");
		assertEquals("undefined", call("$sl", "getA11yRealNode_()"),
				"Slider#getA11yRealNode_ must tolerate a missing -input node");

		// ZK-5725: the three focus_ overrides threw instead of reporting "not focused"
		assertEquals("false", call("$col", "focus_()"),
				"SortWidget#focus_ must tolerate a missing node");
		assertEquals("false", call("$tb", "focus_()"),
				"Tab#focus_ must tolerate a missing node");
		assertEquals("false", call("$mp", "focus_()"),
				"Menupopup#focus_ must tolerate a missing node");

		// ZK-5725: a radio that leaves the DOM through _unbindrod keeps its Radio.unbind_ - and so
		// its _rmExtern - unrun, so it stays in _externs as a ghost without a node
		assertEquals("3", getEval("String(zk.Widget.$('$rg')._externs.length)"),
				"the three radios outside the radiogroup should be registered as externs");
		getEval("(zk.Widget._unbindrod(zk.Widget.$('$rGhost')), 1)");
		assertEquals("3", getEval("String(zk.Widget.$('$rg')._externs.length)"),
				"the un-rod leaves the ghost radio in _externs");

		// detaching another extern radio re-enters _updateAriaOwns, which walked the ghost
		click(jq("$detachOther"));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("the AU response was aborted: " + e.getAlertText());
		}
		assertFalse(hasError(), "a ghost extern radio should not raise an error box");
		assertEquals("2", getEval("String(zk.Widget.$('$rg')._externs.length)"),
				"the detached radio should be gone from _externs");
		assertEquals(getEval("String(zk.Widget.$('$rKeep').uuid + '-real')"),
				jq("$rg").attr("aria-owns"),
				"aria-owns should keep the extern radio that still has a node");

		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}

	private String call(String id, String expr) {
		return getEval("(function(){try{return String(zk.Widget.$('" + id + "')." + expr
				+ ")}catch(e){return 'THROW:' + e}})()");
	}
}
