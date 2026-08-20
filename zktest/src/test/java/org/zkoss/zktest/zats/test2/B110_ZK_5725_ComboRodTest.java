/* B110_ZK_5725_ComboRodTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 18:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * A combobox drops its ROD stub as soon as its popup is drawn for real, so that a comboitem added
 * afterwards still gets a DOM node.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_ComboRodTest extends WebDriverTestCase {

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
		// a closed combobox draws a stub popup and keeps its comboitems in ROD
		assertEquals("true", getEval("String(!!zk.Widget.$('$cb')._rodKid)"),
				"a closed combobox should render its popup as a ROD stub");
		assertEquals(0, comboitemCount(), "the stub popup should hold no comboitem");

		// setPlaceholder() rerenders, and a rerender always draws the popup for real
		click(jq("$rerender"));
		waitResponse();
		assertEquals(2, comboitemCount(), "the rerender should draw the popup for real");
		// ZK-5725: the ROD flag used to survive the real render, which makes shallChildROD_() lie
		assertEquals("false", getEval("String(!!zk.Widget.$('$cb')._rodKid)"),
				"the ROD flag must not survive a real popup render");

		// the server appends a third comboitem, appendChild() must give it a DOM node
		click(jq("$add"));
		waitResponse();
		assertEquals(3, comboitemCount(), "the added comboitem should be rendered");

		click(jq(".z-combobox-button"));
		waitResponse();
		assertEquals(3, jq(".z-combobox-popup .z-comboitem").length(),
				"the open popup should list all three items");
		assertEquals("c", jq(".z-combobox-popup .z-comboitem").eq(2).text(),
				"the added comboitem should be the last one");
		assertNoJSError();
	}

	private int comboitemCount() {
		return Integer.parseInt(
				getEval("String(jq('#' + zk.Widget.$('$cb').uuid + '-pp').find('.z-comboitem').length)"));
	}
}
