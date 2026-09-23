/* B110_ZK_6162Test.java

        Purpose:

        Description:

        History:
                Thu Sep 24 15:18:55 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6162Test extends WebDriverTestCase {

	private static final String BEGIN = "$drb .z-daterangebox-begin";

	private static final String END = "$drb .z-daterangebox-end";

	private static final String REVERSED = "Begin date must not be later than end date";

	private JavascriptExecutor js() {
		return (JavascriptExecutor) driver;
	}

	/** Types the text and blurs, then closes the calendar that focusing opened. */
	private void typeAndCommit(String selector, String text) {
		type(jq(selector), text);
		waitResponse();
		getActions().sendKeys(Keys.ESCAPE).perform();
		waitResponse();
	}

	/** The errorbox opens on a 50ms timer, so it is not up yet when waitResponse returns. */
	private String errorText() {
		new WebDriverWait(driver, Duration.ofSeconds(5))
				.until(d -> jq(".z-errorbox-content").exists());
		return jq(".z-errorbox-content").text();
	}

	private String inputValue(String selector) {
		return (String) js().executeScript("return jq('" + selector + "')[0].value;");
	}

	private void openPopupViaButton() {
		click(jq(".z-daterangebox-button"));
		waitResponse();
	}

	/** Clicks a current-month day cell in the given panel. Day cells keep their
	 *  value in jQuery's data cache, not a DOM attribute, so look it up there. */
	private void clickCellInPanel(int panelIndex, int day) {
		js().executeScript(
				"var panels = document.querySelectorAll('.z-daterangebox-popup-panels .z-calendar');"
				+ "var pane = panels[arguments[0]];"
				+ "if (!pane) return;"
				+ "var cells = pane.querySelectorAll('td.z-calendar-cell');"
				+ "for (var i=0;i<cells.length;i++) {"
				+ "  var v = jq(cells[i]).data('value');"
				+ "  if (v === arguments[1] && (cells[i]._monofs||0) === 0) {"
				+ "    var w = zk.Widget.$(pane);"
				+ "    w._clickDate({target: cells[i], domTarget: cells[i], stop: function(){}});"
				+ "    return;"
				+ "  }"
				+ "}",
				panelIndex, day);
		waitResponse();
	}

	/** The fix: rejection must not destroy what the user typed. */
	@Test
	public void testRejectionKeepsWhatTheUserTyped() {
		connect();
		waitResponse();

		typeAndCommit(BEGIN, "Sep 2, 2026");
		typeAndCommit(END, "Sep 4, 2025");
		errorText(); // let the errorbox settle so the AU response is fully applied

		assertEquals("Sep 2, 2026", inputValue(BEGIN),
				"begin must survive the rejection");
		assertEquals("Sep 4, 2025", inputValue(END),
				"end must survive the rejection — the user has to see the year to fix it");
	}

	/** The server stays authoritative: a rejected pair must not commit. */
	@Test
	public void testRejectedPairDoesNotCommit() {
		connect();
		waitResponse();

		typeAndCommit(BEGIN, "Sep 2, 2026");
		typeAndCommit(END, "Sep 4, 2025");
		errorText();

		Object endValue = js().executeScript(
				"var v = zk.Widget.$('$drb')._endValue; return v ? v.getTime() : null;");
		assertEquals(null, endValue,
				"the rejected end must not reach the committed value, only the input text");
	}

	/** The calendar used to swap an earlier second pick into a valid range while
	 *  the same pair typed in was rejected — one widget, two opposite rules.
	 *  Picking later-then-earlier must now be rejected like the typed path. */
	@Test
	public void testCalendarDoesNotReorderAnEarlierSecondPick() {
		connect();
		waitResponse();

		openPopupViaButton();
		clickCellInPanel(0, 15);
		clickCellInPanel(0, 10);
		sleep(400); // the 200ms auto-apply, plus its round trip

		assertEquals(REVERSED, errorText(),
				"an earlier second pick must be rejected, not silently swapped");
	}
}
