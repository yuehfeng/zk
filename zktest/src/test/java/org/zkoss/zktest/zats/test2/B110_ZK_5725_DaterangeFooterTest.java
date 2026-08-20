/* B110_ZK_5725_DaterangeFooterTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 21:00:00 CST 2026, Created by peakerlee

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
 * The daterange popup draws its Today button only when showTodayLink is on, so bind_() must check
 * the node the way unbind_() already does instead of trusting the flag.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_DaterangeFooterTest extends WebDriverTestCase {

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

		// 1. open the popup once: it is drawn with showTodayLink off, so there is no -today node
		getEval("(zk.Widget.$('$drb')._openPopup(), 1)");
		waitResponse();
		assertEquals("false", getEval("String(!!zk.Widget.$('$drb')._rangePopup.$n('today'))"),
				"the popup should be drawn without a Today button");

		// 2. unbind the popup with its DOM left in place - what an EE *-rod.ts _render() does to
		// its children before rebinding them
		getEval("(zk.Widget.$('$drb')._rangePopup.unbind(), 1)");
		assertEquals("false", getEval("String(!!zk.Widget.$('$drb')._rangePopup.desktop)"),
				"the popup should be unbound now");

		// 3. the server flips showTodayLink; _rebuildFooter cannot see the footer while unbound, so
		// the flag turns true without a -today node ever being created
		click(jq("$todayOn"));
		waitResponse();
		assertEquals("true", getEval("String(zk.Widget.$('$drb').isShowTodayLink())"),
				"the flag should be on after the server flip");
		assertEquals("false", getEval("String(!!zk.Widget.$('$drb')._rangePopup.$n('today'))"),
				"the unbound popup cannot have grown a Today button");

		// 4. ZK-5725 threw "Node with today is not found!" here and aborted the whole bind.
		// The step above leaves an open-but-unbound popup, which keeps the onFloatUp zWatch
		// listener that open() registers and only close() drops, so the click already logged one
		// "Node  is not found!" of its own - a teardown asymmetry that belongs to ZK-6090, not to
		// the node guard under test. Count from here instead of from zero.
		String before = getEval("window.zk5725Errors.length");
		assertEquals("ok", getEval("(function(){var b=zk.Widget.$('$drb');"
						+ "try{b._rangePopup.bind(b.desktop);return 'ok'}catch(e){return 'THROW:' + e}})()"),
				"rebinding the popup must not demand the Today button");
		assertEquals("true", getEval("String(!!zk.Widget.$('$drb')._rangePopup.desktop)"),
				"the popup should be bound again");
		assertEquals(before, getEval("window.zk5725Errors.length"),
				"the rebind threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertFalse(hasError(), "rebinding the popup should not raise an error box");
	}
}
