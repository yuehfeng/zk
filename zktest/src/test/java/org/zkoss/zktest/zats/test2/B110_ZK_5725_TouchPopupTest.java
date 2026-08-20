/* B110_ZK_5725_TouchPopupTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.ExternalZkXml;
import org.zkoss.test.webdriver.ForkJVMTestOnly;
import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The tablet molds place a popup from a 50ms timer whose only guard sits inside an
 * <code>if (zk.android)</code> branch, so on iOS an unbind within that window left the
 * timer dereferencing nodes of a widget that no longer has a DOM.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_TouchPopupTest extends WebDriverTestCase {
	/** An iPhone user agent; it must not carry a "Chrome" token, or zk.ios stays false. */
	private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"
			+ " AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

	/** zktest disables the tablet UI by default, turn it back on. */
	@RegisterExtension
	public static final ExternalZkXml TABLET_UI = new ExternalZkXml("/test2/enable-tablet-ui-zk.xml");

	private static final int TIMER_MS = 400;

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options.setExperimentalOption("mobileEmulation", Map.of(
				"userAgent", USER_AGENT,
				"deviceMetrics", Map.of("width", 390, "height", 844, "pixelRatio", 3, "touch", true)));
	}

	@Test
	public void test() {
		connect();
		waitResponse();
		// the guard that is missing only matters when the Android branch is off
		assertEquals("true", getEval("!!zk.ios"), "zk.ios");
		assertEquals("false", getEval("!!zk.android"), "zk.android");
		assertEquals("true", getEval("!!zk.tabletUIEnabled"), "zk.tabletUIEnabled");

		// the flag alone only says the server allows the tablet molds. The timers under test live
		// in zkmax/touch/**, so prove that each augment really landed on its prototype: neither
		// _initBar nor _syncPosition exists anywhere in CE, so the base prototypes cannot own them.
		assertAugmented("zul.db.Datebox", "zul.db.CalendarPop", "_initBar");
		assertAugmented("zul.db.Timebox", "zul.db.Timebox", "_initBar");
		assertAugmented("zul.inp.Combobox", "zul.inp.Combobox", "_syncPosition");
		assertAugmented("zkmax.inp.Timepicker", "zkmax.inp.Timepicker", "_initBar");

		openAndUnbind("$bdb", "$db", "datebox");
		openAndUnbind("$btbx", "$tbx", "timebox");
		openAndUnbind("$bcb", "$cb", "combobox");
		openAndUnbind("$btp", "$tp", "timepicker");

		assertNoJSError();
	}

	private void assertAugmented(String what, String cls, String member) {
		assertEquals("true",
				getEval("String(Object.prototype.hasOwnProperty.call(" + cls + ".prototype, '" + member + "'))"),
				"the tablet augment of " + what + " should be installed");
	}

	private void openAndUnbind(String button, String widget, String what) {
		getEval("window.zk5725Errors.length = 0");
		assertEquals("true", getEval("String(!!(zk.Widget.$('" + widget + "') || {}).desktop)"),
				"the " + what + " should be bound before the click");
		click(jq(button));
		waitResponse();
		sleep(TIMER_MS);
		// a client throw surfaces as a native alert, which blocks every getEval below
		failOnAlert("opening and unbinding the " + what);
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"opening and unbinding the " + what + " threw: "
						+ getEval("window.zk5725Errors.join(' | ')"));
		// the page opens the popup and unbinds within the 50ms placement window; if the click had
		// done nothing this test would assert nothing at all
		assertEquals("undefined", getEval("String(zk.Widget.$('" + widget + "'))"),
				"the " + what + " should have been unbound by the click");
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
