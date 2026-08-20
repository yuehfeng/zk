/* B110_ZK_5725_HeaderSizingTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 17:30:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Sizing a column re-enables the header filler cell, which a grid drawn without a native scrollbar
 * never had. The same cell is appended later when a column is added, so its cached lookup has to
 * be dropped too.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_HeaderSizingTest extends WebDriverTestCase {
	/** _insizer() answers true within the last 8px of the header cell */
	private static final int INSIDE_RIGHT_BORDER = 3;

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
		// both are needed: no native scrollbar means no filler cell, css flex means it is touched
		assertEquals("false", getEval("String(!!zk.Widget.$('$g')._nativebar)"),
				"the grid should be drawn without a native scrollbar");
		assertEquals("true", getEval("String(!!(zk.Widget.$('$g')._cssflex && zk.Widget.$('$g').isChildrenFlex()))"),
				"the columns should be laid out by css flex");
		assertEquals(0, jq("$cols").find(".z-columns-bar").length(),
				"a grid without a native scrollbar draws no filler cell");

		// 1. dragging the right border of the first column
		int width = columnWidth();
		dragdropTo(jq("$c1"), width / 2 - INSIDE_RIGHT_BORDER, 0, 60, 0);
		waitResponse();
		// ZK-5725 threw "Node with bar is not found!" halfway through the drag
		assertNoError("dragging the column border");

		// 2. double clicking it, which sizes the column to its content the same way
		getActions().moveToElement(toElement(jq("$c1")), columnWidth() / 2 - INSIDE_RIGHT_BORDER, 0)
				.doubleClick().perform();
		waitResponse();
		assertNoError("double clicking the column border");

		// 3. adding a column appends the filler cell, so the cached "not there" has to go
		click(jq("$addcol"));
		waitResponse();
		assertEquals(4, jq("$cols").find(".z-column").length(), "the new column should be drawn");
		assertEquals(1, jq("$cols").find(".z-columns-bar").length(),
				"adding a column appends the filler cell");
		assertNotEquals("n/a", getEval("String(zk.Widget.$('$cols')._subnodes['bar'])"),
				"the appended filler cell should be reachable again");
		assertNoError("adding a column");
	}

	private int columnWidth() {
		return Integer.parseInt(getEval("String(Math.round(zk.Widget.$('$c1').$n().offsetWidth))"));
	}

	/** The throw happens in a DOM event handler, so the page collects it through window.onerror. */
	private void assertNoError(String action) {
		assertEquals("0", getEval("window.zk5725Errors.length"),
				action + " threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}
}
