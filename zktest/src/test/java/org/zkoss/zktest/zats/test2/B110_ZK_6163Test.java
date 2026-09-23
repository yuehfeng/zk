/* B110_ZK_6163Test.java

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

public class B110_ZK_6163Test extends WebDriverTestCase {

	/** SC 1.4.3 for text that is not "large": 18px weight 400 is below the
	 *  18.66px-bold / 24px-regular threshold, so every size needs 4.5:1. */
	private static final double AA = 4.5d;

	private JavascriptExecutor js() {
		return (JavascriptExecutor) driver;
	}

	/**
	 * Composites the element's colour over its background (both may be rgba)
	 * and returns the WCAG 2.1 contrast ratio. Computed values are read from
	 * the live node, so this follows whatever palette the page resolved to
	 * rather than re-stating the hex codes the LESS happens to use today.
	 */
	private double contrast(String widgetId) {
		return ((Number) js().executeScript(
				"var el = zk.Widget.$('$" + widgetId + "').$n();"
				+ "function rgb(s) { var m = (s || '').match(/[\\d.]+/g) || [0,0,0];"
				+ "  return {r:+m[0], g:+m[1], b:+m[2], a:m.length>3?+m[3]:1}; }"
				+ "function over(f, b) { return {r:f.r*f.a+b.r*(1-f.a), g:f.g*f.a+b.g*(1-f.a),"
				+ "  b:f.b*f.a+b.b*(1-f.a), a:1}; }"
				+ "function lum(c) { function f(v) { v/=255;"
				+ "  return v<=0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4); }"
				+ "  return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }"
				+ "var cs = getComputedStyle(el), page = {r:255,g:255,b:255,a:1};"
				+ "var bg = over(rgb(cs.backgroundColor), page), fg = over(rgb(cs.color), bg);"
				+ "var l1 = lum(fg), l2 = lum(bg);"
				+ "if (l1 < l2) { var t = l1; l1 = l2; l2 = t; }"
				+ "return (l1 + 0.05) / (l2 + 0.05);")).doubleValue();
	}

	private void assertAA(String widgetId, String what) {
		double c = contrast(widgetId);
		assertTrue(c >= AA,
				what + " must reach " + AA + ":1 for SC 1.4.3; measured " + c + ":1");
	}

	@Test
	public void testInitialsReachAAAtEverySize() {
		connect();
		waitResponse();

		assertAA("avSmall", "small avatar initials");
		assertAA("avMedium", "medium avatar initials");
		assertAA("avLarge", "large avatar initials");
	}

	/** The glyph variant paints on the same pair, so it must clear the bar too. */
	@Test
	public void testIconVariantReachesAA() {
		connect();
		waitResponse();

		assertAA("avIcon", "iconSclass avatar glyph");
	}

	/** Control: the overflow avatar has its own token pair and already passed —
	 *  pinned so a future palette edit cannot quietly drag it under the bar. */
	@Test
	public void testAvatargroupOverflowStillReachesAA() {
		connect();
		waitResponse();

		double c = ((Number) js().executeScript(
				"var el = document.querySelector('.z-avatargroup-overflow');"
				+ "function rgb(s) { var m = (s || '').match(/[\\d.]+/g) || [0,0,0];"
				+ "  return {r:+m[0], g:+m[1], b:+m[2], a:m.length>3?+m[3]:1}; }"
				+ "function over(f, b) { return {r:f.r*f.a+b.r*(1-f.a), g:f.g*f.a+b.g*(1-f.a),"
				+ "  b:f.b*f.a+b.b*(1-f.a), a:1}; }"
				+ "function lum(c) { function f(v) { v/=255;"
				+ "  return v<=0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4); }"
				+ "  return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }"
				+ "var cs = getComputedStyle(el), page = {r:255,g:255,b:255,a:1};"
				+ "var bg = over(rgb(cs.backgroundColor), page), fg = over(rgb(cs.color), bg);"
				+ "var l1 = lum(fg), l2 = lum(bg);"
				+ "if (l1 < l2) { var t = l1; l1 = l2; l2 = t; }"
				+ "return (l1 + 0.05) / (l2 + 0.05);")).doubleValue();
		assertTrue(c >= AA,
				"avatargroup overflow must reach " + AA + ":1; measured " + c + ":1");
	}
}
