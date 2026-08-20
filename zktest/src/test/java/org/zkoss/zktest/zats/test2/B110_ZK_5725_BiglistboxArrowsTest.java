/* B110_ZK_5725_BiglistboxArrowsTest.java

	Purpose:

	Description:

	History:
		Mon Aug 25 10:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Walking a biglistbox sideways scrolls it, and every scroll makes the a11y layer look the column
 * and row blocks up again. Without frozenCols the frozen blocks are not in the DOM, so those
 * lookups have to tolerate a missing node. Walking down past the drawn rows asks the widget itself
 * whether a row that was never drawn is in view.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_BiglistboxArrowsTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// the console is only exposed to the test when Chrome is asked for it
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	/**
	 * ZK-5725 threw "Node with cols is not found!" as soon as a sideways walk scrolled the grid,
	 * because the handler re-read the blocks through $n_ after every _doScrollX.
	 */
	@Test
	public void testWalkSideways() {
		connect();
		// the walk under test only exists in the za11y layer. A silent return would make the NO_A11Y run report a
		// pass, so abort the test instead and let it show up as skipped.
		Assumptions.assumeTrue(Boolean.parseBoolean(getEval("!!window.za11y")),
				"the za11y layer is not loaded, the keyboard navigation under test does not exist");
		assertSetup();

		int visible = Integer.parseInt(getEval("String(zk.Widget.$('$blb')._getVisibleCols())"));
		focusFirstCell();
		// clicking a row parks the focus on the anchor; the up arrow is what puts it on a real cell
		getActions().sendKeys(Keys.ARROW_UP).perform();
		waitResponse();
		assertEquals("0,0", getEval("String(document.activeElement.getAttribute('data-axis'))"),
				"the up arrow should put the focus on the first column header");

		// walk one column past the last visible one, which is the first _doScrollX of the run
		for (int i = 0; i <= visible; i++) {
			getActions().sendKeys(Keys.ARROW_RIGHT).perform();
			waitResponse();
			assertNoJsError("walking right, step " + (i + 1));
		}
		assertNotEquals("0", getEval("String(zk.Widget.$('$blb')._currentX)"),
				"walking past the last visible column should have scrolled the grid sideways");

		// walk back the other way, which scrolls again and finally falls off the left edge - the
		// step where the handler looks for the frozen block that is not there
		int scrolled = Integer.parseInt(getEval("String(zk.Widget.$('$blb')._currentX)"));
		for (int i = 0; i < scrolled + 3; i++) {
			getActions().sendKeys(Keys.ARROW_LEFT).perform();
			waitResponse();
			assertNoJsError("walking left, step " + (i + 1));
		}
		// Where the focus lands is deliberately not asserted: without frozen columns the handler
		// leaves both of its branches alone at the edge, so the browser default takes over and the
		// landing spot is not part of the contract. Not throwing on the way is.
	}

	/**
	 * ZK-5725 read .offsetHeight off the row it did not find, so moving the selection below the
	 * drawn window threw "Cannot read properties of undefined".
	 */
	@Test
	public void testWalkBelowTheDrawnRows() {
		connect();
		assertSetup();
		focusFirstCell();

		getActions().sendKeys(Keys.ARROW_UP).perform();
		waitResponse();
		int drawn = Integer.parseInt(getEval("String(zk.Widget.$('$blb').$n('rows').rows.length)"));
		assertNotEquals(0, drawn, "the page should draw a window of rows, not the whole model");
		// step past the last drawn row, so _isInView is asked about a row that has no element
		for (int i = 0; i <= drawn; i++) {
			getActions().sendKeys(Keys.ARROW_DOWN).perform();
			waitResponse();
			assertNoJsError("walking down, step " + (i + 1));
		}
		assertNotEquals("0", getEval("String(zk.Widget.$('$blb')._currentY)"),
				"walking below the drawn rows should have scrolled the list down");
	}

	private void assertSetup() {
		assertEquals("0", getEval("String(zk.Widget.$('$blb')._frozenCols)"));
		assertEquals("false", getEval("String(!!zk.Widget.$('$blb').$n('colsfx'))"),
				"the frozen header block is not in the DOM");
		assertEquals("false", getEval("String(!!zk.Widget.$('$blb').$n('rowsfx'))"),
				"the frozen row block is not in the DOM");
	}

	private void focusFirstCell() {
		click(jq("$blb").find("td").eq(0));
		waitResponse();
		assertEquals("0", getEval("String(zk.Widget.$('$blb')._selItems[0][1])"),
				"the first row should be the selected one");
	}

	private void assertNoJsError(String action) {
		assertEquals("0", getEval("window.zk5725Errors.length"),
				action + " threw: " + getEval("window.zk5725Errors.join(' | ')"));
	}
}
