/* B110_ZK_6164Test.java

        Purpose:

        Description:

        History:
                Thu Sep 24 15:18:55 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6164Test extends WebDriverTestCase {

	private JavascriptExecutor js() {
		return (JavascriptExecutor) driver;
	}

	private void openPopup() {
		click(jq("$opener"));
		waitResponse();
	}

	private String styleOf(String selector, String prop) {
		return (String) js().executeScript(
				"return getComputedStyle(document.querySelector('" + selector + "'))"
				+ ".getPropertyValue('" + prop + "');");
	}

	/** Moves the token, then reads it back off the node. Comparing the popup's
	 *  computed value against the plain button's cannot tell a wired-up token
	 *  from a literal that happens to match today; moving it can. */
	private void overrideToken(String name, String value) {
		js().executeScript("document.documentElement.style.setProperty('" + name + "', '" + value + "');");
	}

	@Test
	public void testButtonsFollowTheButtonTokens() {
		connect();
		waitResponse();
		openPopup();

		overrideToken("--zk-button-padding", "13px 29px");
		overrideToken("--zk-button-border-radius", "7px");
		overrideToken("--zk-button-background-color", "rgb(1, 2, 3)");

		for (String sel : new String[] {".z-confirmpopup-ok", ".z-confirmpopup-cancel"}) {
			assertEquals("13px 29px", styleOf(sel, "padding"),
					sel + " must take its padding from --zk-button-padding");
			assertEquals("7px", styleOf(sel, "border-radius"),
					sel + " must take its radius from --zk-button-border-radius");
			assertEquals("rgb(1, 2, 3)", styleOf(sel, "background-color"),
					sel + " must take its fill from --zk-button-background-color");
		}
	}

	/** Same tokens in, same geometry out as a plain button on the page. */
	@Test
	public void testButtonsMatchThePlainButtonGeometry() {
		connect();
		waitResponse();
		openPopup();

		String plainPadding = styleOf(".z-button", "padding");
		String plainMinHeight = styleOf(".z-button", "min-height");
		String plainRadius = styleOf(".z-button", "border-radius");

		for (String sel : new String[] {".z-confirmpopup-ok", ".z-confirmpopup-cancel"}) {
			assertEquals(plainPadding, styleOf(sel, "padding"), sel + " padding must match .z-button");
			assertEquals(plainMinHeight, styleOf(sel, "min-height"), sel + " min-height must match .z-button");
			assertEquals(plainRadius, styleOf(sel, "border-radius"), sel + " radius must match .z-button");
		}
	}

	/** The shadow is what makes it read as a button at a glance. */
	@Test
	public void testButtonsCarryTheButtonShadow() {
		connect();
		waitResponse();
		openPopup();

		String plain = styleOf(".z-button", "box-shadow");
		for (String sel : new String[] {".z-confirmpopup-ok", ".z-confirmpopup-cancel"}) {
			assertEquals(plain, styleOf(sel, "box-shadow"), sel + " must carry the button shadow");
		}
	}
}
