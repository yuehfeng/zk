/* B110_ZK_5725_BiglistboxKeysTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Biglistbox draws its frozen column and row blocks only when frozenCols is set, so the a11y
 * keyboard navigation must not demand them on a plain biglistbox.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_BiglistboxKeysTest extends WebDriverTestCase {

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
		// the keyboard navigation under test only exists in the za11y layer. A silent return would make the NO_A11Y run
		// report a pass, so abort the test instead and let it show up as skipped.
		Assumptions.assumeTrue(Boolean.parseBoolean(getEval("!!window.za11y")),
				"the za11y layer is not loaded, the keyboard navigation under test does not exist");

		// the setup: no frozen columns, so neither frozen block is in the DOM
		assertEquals("0", getEval("String(zk.Widget.$('$blb')._frozenCols)"));
		assertEquals("false", getEval("String(!!zk.Widget.$('$blb').$n('colsfx'))"),
				"the frozen header block is not in the DOM");
		assertEquals("false", getEval("String(!!zk.Widget.$('$blb').$n('rowsfx'))"),
				"the frozen row block is not in the DOM");

		click(jq("$blb").find("td").eq(0));
		waitResponse();
		assertEquals("0", getEval("String(zk.Widget.$('$blb')._selItems[0][1])"),
				"the first row should be the selected one");
		assertEquals("true", getEval("String(document.activeElement == zk.Widget.$('$blb').$n('a'))"),
				"selecting a row moves the focus to the anchor of the biglistbox");

		// ZK-5725 threw "Node with colsfx is not found!" here and killed the keyboard handling
		getActions().sendKeys(Keys.ARROW_UP).perform();
		waitResponse();
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertEquals("0,0", getEval("String(document.activeElement.getAttribute('data-axis'))"),
				"the up arrow should move the focus to the first column header");

		// walking on from a cell reaches the second dereference of the frozen row block
		getActions().sendKeys(Keys.ARROW_RIGHT, Keys.ARROW_DOWN).perform();
		waitResponse();
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
	}
}
