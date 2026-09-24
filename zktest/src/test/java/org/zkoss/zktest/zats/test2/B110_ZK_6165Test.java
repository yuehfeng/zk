/* B110_ZK_6165Test.java

        Purpose:

        Description:

        History:
                Thu Sep 24 15:18:55 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6165Test extends WebDriverTestCase {

	/** SC 1.4.11 Non-text Contrast, for an essential graphical object. */
	private static final double AA = 3.0d;

	private JavascriptExecutor js() {
		return (JavascriptExecutor) driver;
	}

	/**
	 * Contrast of the chevron against the disc it is drawn on, with the disc's
	 * rgba backdrop composited over the slide actually behind it. Reading the
	 * slide rather than assuming white is what makes this fail before the fix
	 * and pass after, on the page's own light slide.
	 */
	private double chevronContrast(String arrowClass) {
		return ((Number) js().executeScript(
				"var a = document.querySelector('" + arrowClass + "');"
				+ "var slide = document.getElementById('lightSlide');"
				+ "function rgb(s) { var m = (s || '').match(/[\\d.]+/g) || [0,0,0];"
				+ "  return {r:+m[0], g:+m[1], b:+m[2], a:m.length>3?+m[3]:1}; }"
				+ "function over(f, b) { return {r:f.r*f.a+b.r*(1-f.a), g:f.g*f.a+b.g*(1-f.a),"
				+ "  b:f.b*f.a+b.b*(1-f.a), a:1}; }"
				+ "function lum(c) { function f(v) { v/=255;"
				+ "  return v<=0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4); }"
				+ "  return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }"
				+ "var page = {r:255,g:255,b:255,a:1};"
				+ "var back = over(rgb(getComputedStyle(slide).backgroundColor), page);"
				+ "var disc = over(rgb(getComputedStyle(a).backgroundColor), back);"
				+ "var chev = over(rgb(getComputedStyle(a).color), disc);"
				+ "var l1 = lum(chev), l2 = lum(disc);"
				+ "if (l1 < l2) { var t = l1; l1 = l2; l2 = t; }"
				+ "return (l1 + 0.05) / (l2 + 0.05);")).doubleValue();
	}

	@Test
	public void testPrevArrowChevronReachesNonTextContrast() {
		connect();
		waitResponse();

		double c = chevronContrast(".z-carousel-arrow-prev");
		assertTrue(c >= AA,
				"the prev chevron must reach " + AA + ":1 on a light slide; measured " + c + ":1");
	}

	@Test
	public void testNextArrowChevronReachesNonTextContrast() {
		connect();
		waitResponse();

		double c = chevronContrast(".z-carousel-arrow-next");
		assertTrue(c >= AA,
				"the next chevron must reach " + AA + ":1 on a light slide; measured " + c + ":1");
	}

	/**
	 * Contrast cannot make the disc findable on a dark slide — black on black at
	 * any alpha — so the control carries its own ring, as zkmax scrollview's
	 * floating scrollbar does. Pinned because an alpha-only fix looks correct on
	 * the light slide while leaving the dark one unusable.
	 */
	@Test
	public void testArrowCarriesARingSoItSurvivesADarkSlide() {
		connect();
		waitResponse();

		String shadow = (String) js().executeScript(
				"return getComputedStyle(document.querySelector('.z-carousel-arrow-prev')).boxShadow;");
		assertFalse(shadow == null || shadow.isEmpty() || "none".equals(shadow),
				"the arrow must carry a ring; box-shadow was '" + shadow + "'");

		double ringVsDisc = ((Number) js().executeScript(
				"var a = document.querySelector('.z-carousel-arrow-prev');"
				+ "var slide = document.getElementById('darkSlide');"
				+ "function rgb(s) { var m = (s || '').match(/[\\d.]+/g) || [0,0,0];"
				+ "  return {r:+m[0], g:+m[1], b:+m[2], a:m.length>3?+m[3]:1}; }"
				+ "function over(f, b) { return {r:f.r*f.a+b.r*(1-f.a), g:f.g*f.a+b.g*(1-f.a),"
				+ "  b:f.b*f.a+b.b*(1-f.a), a:1}; }"
				+ "function lum(c) { function f(v) { v/=255;"
				+ "  return v<=0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4); }"
				+ "  return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }"
				+ "var page = {r:255,g:255,b:255,a:1};"
				+ "var back = over(rgb(getComputedStyle(slide).backgroundColor), page);"
				+ "var disc = over(rgb(getComputedStyle(a).backgroundColor), back);"
				// the ring colour is the last rgba in the computed box-shadow
				+ "var m = getComputedStyle(a).boxShadow.match(/rgba?\\([^)]*\\)/g) || [];"
				+ "var ring = over(rgb(m[m.length - 1]), disc);"
				+ "var l1 = lum(ring), l2 = lum(disc);"
				+ "if (l1 < l2) { var t = l1; l1 = l2; l2 = t; }"
				+ "return (l1 + 0.05) / (l2 + 0.05);")).doubleValue();
		assertTrue(ringVsDisc >= AA,
				"the ring must reach " + AA + ":1 against the disc on a dark slide; measured "
				+ ringVsDisc + ":1");
	}
}
