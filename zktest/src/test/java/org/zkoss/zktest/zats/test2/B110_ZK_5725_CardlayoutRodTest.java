/* B110_ZK_5725_CardlayoutRodTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * The tablet fork of Cardlayout repositions the siblings of a removed card without ever
 * checking that the cardlayout has a DOM, which a render-deferred container does not give it.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_CardlayoutRodTest extends WebDriverTestCase {
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
		// the flag alone only says the server allows the tablet molds, so check that the augment
		// of zkmax/touch/cardlayout-touch.ts really landed on the prototype. bindSwipe_ is a
		// no-op on zk.Widget.prototype, so only the augment makes it an own property here.
		assertEquals("true", getEval("!!zk.tabletUIEnabled"), "zk.tabletUIEnabled");
		assertEquals("true",
				getEval("String(Object.prototype.hasOwnProperty.call(zkmax.layout.Cardlayout.prototype, 'bindSwipe_'))"),
				"the tablet augment of Cardlayout should be installed");
		// the cardlayout is reachable by the client but was never drawn
		assertEquals("true", getEval("String(!!zk.Widget.$('$cl').z_rod)"), "cardlayout.z_rod");
		assertEquals("undefined", getEval("String(zk.Widget.$('$cl').desktop)"), "cardlayout.desktop");

		click(jq("$rm"));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("removing a card failed: " + e.getAlertText());
		}
		// the alert can also arrive after the response settled, which the catch above misses
		failOnAlert("removing a card");
		assertFalse(hasError(), "removing a card failed: " + jq(".z-error").text());
		// ZK-5725: the throw aborted the rest of the response, so this update never arrived
		assertEquals("removed", jq("$done").text(), "the rest of the response should still apply");
		assertEquals("2", getEval("String(zk.Widget.$('$cl').nChildren)"), "the card should be gone");
		assertNoJSError();
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
