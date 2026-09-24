/* B110_ZK_6166Test.java

        Purpose:

        Description:

        History:
                Thu Sep 24 15:18:55 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6166Test extends WebDriverTestCase {

	private JavascriptExecutor js() {
		return (JavascriptExecutor) driver;
	}

	/**
	 * How far the indicator pokes out of its nearest clipping ancestor, in px.
	 * getBoundingClientRect alone cannot answer this — a clipped box still
	 * reports its full rect — so the caller pairs it with a hit test.
	 */
	private double clippedBy(String badgeId) {
		return ((Number) js().executeScript(
				"var el = zk.Widget.$('$" + badgeId + "').$n()"
				+ "  .querySelector('.z-badge-indicator');"
				+ "var clip = null;"
				+ "for (var p = el.parentElement; p && p !== document.body; p = p.parentElement) {"
				+ "  var cs = getComputedStyle(p);"
				+ "  if (cs.overflow !== 'visible' || cs.overflowY !== 'visible') { clip = p; break; }"
				+ "}"
				+ "if (!clip) return 0;"
				+ "var r = el.getBoundingClientRect(), c = clip.getBoundingClientRect();"
				+ "return Math.max(0, c.top - r.top, r.bottom - c.bottom,"
				+ "                c.left - r.left, r.right - c.right);")).doubleValue();
	}

	/** Paint-level check: is the indicator's top edge actually drawn there? */
	private boolean topEdgeVisible(String badgeId) {
		return (Boolean) js().executeScript(
				"var el = zk.Widget.$('$" + badgeId + "').$n()"
				+ "  .querySelector('.z-badge-indicator');"
				+ "var r = el.getBoundingClientRect();"
				+ "var stack = document.elementsFromPoint(r.left + r.width / 2, r.top + 2);"
				+ "return stack.indexOf(el) >= 0;");
	}

	private void assertWhole(String badgeId, String what) {
		double cut = clippedBy(badgeId);
		assertTrue(cut == 0, what + " must not be clipped; " + cut + "px is outside its container");
		assertTrue(topEdgeVisible(badgeId), what + "'s top edge must actually be painted");
	}

	/** The ticket's own repro: a plain groupbox, no author CSS. */
	@Test
	public void testIndicatorSurvivesAGroupbox() {
		connect();
		waitResponse();

		assertWhole("bInbox", "a count badge in a groupbox");
	}

	/** A wider indicator ("99+") overhangs further, so it is the harder case. */
	@Test
	public void testWideIndicatorSurvivesAGroupbox() {
		connect();
		waitResponse();

		assertWhole("bAlerts", "an overflowed count badge in a groupbox");
	}

	/** Dot mode is 6px, so it reserves less — its own arithmetic. */
	@Test
	public void testDotSurvivesAGroupbox() {
		connect();
		waitResponse();

		assertWhole("bDot", "a dot badge in a groupbox");
	}

	/** Groupbox is only the reported case; any overflow: hidden box did it. */
	@Test
	public void testIndicatorSurvivesAPlainClippingDiv() {
		connect();
		waitResponse();

		assertWhole("bTasks", "a count badge in an overflow: hidden div");
	}
}
