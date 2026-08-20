/* B110_ZK_5725_BiglistboxResizeTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Biglistbox draws the frozen head shim only when the model has a header row, so onSize() must
 * not demand it just because frozenCols is set.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_BiglistboxResizeTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// the console is only exposed to the test when Chrome is asked for it
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	@Test
	public void test() {
		connect();
		// the setup that makes the head shim absent while its four frozen siblings are drawn
		assertEquals("0", getEval("String(zk.Widget.$('$blb').getColData().length)"),
				"the model has no header row, so no head is drawn");
		assertEquals("2", getEval("String(zk.Widget.$('$blb')._frozenCols)"),
				"the frozen columns are what onSize() reacts to");
		assertEquals("false", getEval("String(!!zk.Widget.$('$blb').$n('headshim'))"),
				"the frozen head shim is not in the DOM");
		assertEquals("true", getEval("String(!!zk.Widget.$('$blb').$n('bodyshim'))"),
				"the frozen body shim is in the DOM");

		// ZK-5725 threw "Node with headshim is not found!" out of the first onSize pass, which
		// already runs during the mount and leaves the page stuck behind the processing mask
		assertMounted();
		assertLaidOut();

		click(jq("$resize"));
		waitResponse();
		assertFalse(hasError(), "the resize failed: " + jq(".z-messagebox-window").text());
		assertMounted();
		assertLaidOut();
	}

	private void assertMounted() {
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertEquals("0", getEval("String(jq('#zk_proc-m').length)"),
				"the page should not be stuck behind the processing mask");
	}

	private void assertLaidOut() {
		// the last statement of the frozen branch of onSize(), so it only holds once the whole
		// branch has run
		assertNotEquals("", getEval("zk.Widget.$('$blb').$n('vbartick').style.left"),
				"onSize() should have placed the vertical bar tick");
		assertNotEquals("", getEval("zk.Widget.$('$blb').$n('body-cnt').style.width"),
				"onSize() should have sized the scrolling body");
	}
}
