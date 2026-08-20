/* B110_ZK_5725_TreeScrollTest.java

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
 * The touch fork of Tree scrolls the selected item into view from a timer. The item comes
 * out of the flattened iterator, which also yields items that were never drawn, and the
 * only guard is that the item exists as a widget.
 *
 * @author peakerlee
 */
@ForkJVMTestOnly
public class B110_ZK_5725_TreeScrollTest extends WebDriverTestCase {
	/** An Android tablet user agent, so that zk.mobile is true. */
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-X710)"
			+ " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	/** zktest disables the tablet UI by default, turn it back on. */
	@RegisterExtension
	public static final ExternalZkXml TABLET_UI = new ExternalZkXml("/test2/enable-tablet-ui-zk.xml");

	private static final int TIMER_MS = 600;

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
		assertEquals("true", getEval("!!zk.touchEnabled"), "zk.touchEnabled");
		assertEquals("false", getEval("String(zk.Widget.$('$t')._nativebar)"), "tree._nativebar");
		// the selected item is the one that was never drawn
		assertEquals("1", getEval("String(zk.Widget.$('$t').getSelectedIndex())"), "selectedIndex");
		String items = getEval("zk.Widget.$('$t').getItems().map(function (i) {"
				+ " return (i.id || '-') + '/' + (i.desktop ? 'bound' : 'rod')"
				+ " + '/rd=' + i._renderdefer + '/rod=' + i.z_rod + '/n=' + !!i.$n() }).join(' ')");
		assertEquals("false", getEval("String(!!zk.Widget.$('$t').getItems()[1].$n())"),
				"the selected item should have no DOM; items = " + items);

		sleep(TIMER_MS);
		click(jq("$rz"));
		waitResponse();
		sleep(TIMER_MS);

		// a client throw surfaces as a native alert, which blocks every getEval below
		failOnAlert("scrolling the selection into view");
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"scrolling the selection into view threw: "
						+ getEval("window.zk5725Errors.join(' | ')"));
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
