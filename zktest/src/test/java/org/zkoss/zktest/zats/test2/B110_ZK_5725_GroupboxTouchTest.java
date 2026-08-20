/* B110_ZK_5725_GroupboxTouchTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.ExternalZkXml;
import org.zkoss.test.webdriver.ForkJVMTestOnly;
import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The tablet mold of Groupbox forks the ROD render, so it needs the same ZK-5681 guard: a
 * render-deferred child only has a placeholder and must not be rebound.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_GroupboxTouchTest extends WebDriverTestCase {
	/** An Android tablet user agent, so that zk.mobile is true. */
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-X710)"
			+ " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	/** zktest disables the tablet UI by default, turn it back on. */
	@RegisterExtension
	public static final ExternalZkXml TABLET_UI = new ExternalZkXml("/test2/enable-tablet-ui-zk.xml");


	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options.setExperimentalOption("mobileEmulation", Map.of(
				"userAgent", USER_AGENT,
				"deviceMetrics", Map.of("width", 1024, "height", 768, "pixelRatio", 2, "touch", true)));
	}

	@Test
	public void test() {
		connect();
		waitResponse();
		// without the tablet molds this test would exercise the desktop code, which is already
		// fixed. The flag alone only says the server allows them, so check that the augment of
		// zkmax/touch/groupbox-touch.ts really landed on the prototype: _refresh exists nowhere
		// in CE, so the base prototype cannot own it.
		assertEquals("true", getEval("!!zk.tabletUIEnabled"), "zk.tabletUIEnabled");
		assertEquals("true",
				getEval("String(Object.prototype.hasOwnProperty.call(zul.wgt.Groupbox.prototype, '_refresh'))"),
				"the tablet augment of Groupbox should be installed");
		assertEquals("1", getEval("jq('$gb [id$=\"-cave2\"]').length"),
				"only the tablet mold of Groupbox renders a -cave2 node");
		assertEquals("true", getEval("String(!!zk.Widget.$('$gb')._rodKid)"),
				"a closed groupbox should render its content as a ROD stub");

		// ZK-5725: the tablet fork of _render() rebound the deferred checkbox, whose placeholder
		// has no -real node, so Checkbox.bind_() died on "reading 'defaultChecked'"
		click(jq("$op"));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("opening the groupbox failed: " + e.getAlertText());
		}
		// the alert can also arrive after the response settled, which the catch above misses
		failOnAlert("opening the groupbox");
		assertFalse(hasError(), "opening the groupbox failed: " + jq(".z-error").text());
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"opening the groupbox threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoJSError();

		assertTrue(jq("$inside").exists(), "the groupbox content should be drawn");
		assertTrue(jq("$inside").isVisible(), "the groupbox should be open");
		// the deferred checkbox keeps its placeholder, so it is not bound and jq('$cbx') finds
		// nothing - the node has to be looked up by uuid
		assertEquals("true", getEval("String(!!zk.Widget.$('$cbx')._z$rd)"),
				"the deferred checkbox should still be waiting for its render");
		assertEquals("z-renderdefer",
				getEval("document.getElementById(zk.Widget.$('$cbx').uuid).className"),
				"the deferred checkbox should still be a placeholder");
	}

	/**
	 * Fails with the text of the native alert the AU loop raises for a client side throw, if there
	 * is one. Such an alert also blocks every later WebDriver call, so read it before asserting
	 * anything else.
	 */
	private void failOnAlert(String what) {
		Alert alert;
		try {
			alert = getWebDriver().switchTo().alert();
		} catch (NoAlertPresentException e) {
			return;
		}
		String text = alert.getText();
		alert.dismiss();
		fail(what + " failed: " + text);
	}
}
