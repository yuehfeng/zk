/* B110_ZK_5725_ToolbarMoldTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 15:20:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;
import org.zkoss.test.webdriver.ztl.JQuery;

/**
 * Toolbar renders the overflow popup button and the popup in its default mold only, so
 * {@code bind_()} must not demand those two nodes just because overflowPopup is on.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_ToolbarMoldTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	@Test
	public void test() {
		connect();
		// ZK-5725 threw "Node with overflowpopup-button is not found!" and aborted the whole mount
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"the mount threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
		waitResponse();

		// 1. the panel mold has no overflow popup at all, but its content is still drawn
		assertTrue(jq("$tbPanel").exists(), "the panel mold toolbar should be rendered");
		assertEquals(3, jq("$tbPanel").find(".z-toolbarbutton").length(),
				"the panel mold toolbar should keep its buttons");
		assertFalse(overflowButton("$tbPanel").exists(),
				"the panel mold renders no overflow popup button");

		// 2. a tabbox with tabscroll=false never draws its toolbar, but the tabbox itself must work
		assertTrue(jq("$tbx").exists(), "the tabbox should be rendered");
		assertFalse(jq("$tbTabbox").exists(), "a tabbox with tabscroll=false draws no toolbar");
		assertEquals("true", getEval("!!zk.Widget.$('$tbTabbox')"),
				"the toolbar widget is still bound even without a DOM node");
		assertTrue(jq("$panel1").isVisible());
		click(jq("$tab2"));
		waitResponse();
		assertTrue(jq("$panel2").isVisible(), "the tabbox should still switch panels");

		// 3. the default mold keeps a working overflow popup
		JQuery button = overflowButton("$tbDefault");
		assertTrue(button.exists(), "the default mold renders the overflow popup button");
		click(button);
		waitResponse();
		// _openPopup() makes the popup a virtual child of the body
		assertTrue(jq(".z-toolbar-popup-open").exists(), "clicking the button should open the popup");

		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}

	private JQuery overflowButton(String id) {
		return jq(id).find(".z-toolbar-overflowpopup-button");
	}
}
