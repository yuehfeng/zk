/* B110_ZK_5725_PopupRodTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * A menupopup buried in a closed groupbox has no node of its own, so opening it has to render the
 * ROD container first instead of replacing a node that was never drawn.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_PopupRodTest extends WebDriverTestCase {

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
		// the groupbox is a ROD container: it flags _rodKid, its children get z_rod and no DOM
		assertEquals("true", getEval("String(!!zk.Widget.$('$gb')._rodKid)"),
				"a closed groupbox should render its content as a ROD stub");
		assertEquals("true", getEval("String(!!zk.Widget.$('$mp').z_rod)"),
				"the menupopup should be in ROD");
		assertEquals("false", getEval("String(!!document.getElementById(zk.Widget.$('$mp').uuid))"),
				"the menupopup should have no DOM node yet");

		// ZK-5725: rodRender() replaced a node that does not exist - a silent no-op followed by a
		// bind() that dies on $n_(), reported as "Uncaught Node  is not found!"
		click(jq("$op"));
		waitResponse();
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"opening the popup threw: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoJSError();

		assertTrue(jq("$mp").exists(), "the menupopup should be rendered");
		assertEquals(2, jq("$mp").find(".z-menuitem").length(),
				"the menupopup should list its two menu items");
		assertTrue(jq("$mp").isVisible(), "the menupopup should be open");
	}
}
