/* B110_ZK_5725_OrganigramRodTest.java

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
 * An orgchildren drops its ROD stub as soon as its children are drawn for real, so that opening
 * the orgitem does not append a second copy of every child.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_OrganigramRodTest extends WebDriverTestCase {

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
		// a closed orgitem keeps its children in ROD, the container flags itself with _rodKid
		assertEquals("true", getEval("String(!!zk.Widget.$('$oc')._rodKid)"),
				"a closed orgitem should render its children as a ROD stub");
		assertEquals(0, childCount(), "the stub should hold no orgitem");

		// setZclass() rerenders, and a rerender always draws the children for real
		click(jq("$rerender"));
		waitResponse();
		assertEquals(2, childCount(), "the rerender should draw the children for real");
		// ZK-5725: the ROD flag used to survive the real render
		assertEquals("false", getEval("String(!!zk.Widget.$('$oc')._rodKid)"),
				"the ROD flag must not survive a real render of the children");

		// _render() used to append a second copy of every child on top of the ones already drawn
		click(jq("$open"));
		waitResponse();
		assertEquals(2, childCount(), "opening the orgitem should not duplicate its children");
		assertEquals(2, jq("$oc").find(".z-orgnode").length(),
				"each child should be rendered exactly once");
		assertNoJSError();
	}

	private int childCount() {
		return Integer.parseInt(getEval("String(jq('$oc').children('.z-orgitem').length)"));
	}
}
