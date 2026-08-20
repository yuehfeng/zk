/* B110_ZK_5725_NestedRodTest.java

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
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The ROD render of a groupbox only asked whether its parent is in ROD. An unbound groupbox has no
 * cave to render into either, so it has to be left alone.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_NestedRodTest extends WebDriverTestCase {

	@Override
	protected ChromeOptions getWebDriverOptions() {
		ChromeOptions options = super.getWebDriverOptions();
		// assertNoJSError() reads the browser console, which Chrome only exposes when asked for
		options.setCapability("goog:loggingPrefs", Collections.singletonMap(LogType.BROWSER, Level.ALL));
		return options;
	}

	/**
	 * No UI path is known that unbinds a groupbox while its ROD state stays behind - a server side
	 * invalidate() replaces the widgets instead - so this one sets the state up by hand and checks
	 * the contract: an unbound container renders nothing.
	 */
	@Test
	public void testUnboundGroupboxKeepsItsRodState() {
		connect();
		assertEquals("true", getEval("String(!!zk.Widget.$('$inner')._rodKid)"),
				"the closed inner groupbox should render its content as a ROD stub");
		assertEquals("false", getEval("String(!!zk.Widget.$('$sb').desktop)"),
				"the selectbox of a closed groupbox should be in ROD");

		getEval("(function(){zk.Widget.$('$inner').desktop = undefined; return 1})()");
		// ZK-5725: _render() went on to replace a cave that is not in the document and to rebind
		// the selectbox against nothing
		getEval("(function(){zk.Widget.$('$inner').setOpen(true); return 1})()");

		assertEquals("true", getEval("String(!!zk.Widget.$('$inner')._rodKid)"),
				"an unbound groupbox must keep its ROD state instead of rendering into nothing");
		assertEquals("false", getEval("String(!!zk.Widget.$('$sb').desktop)"),
				"the selectbox of an unbound groupbox must not be rebound");
		assertNoJSError();
	}

	@Test
	public void testNestedRodStillRenders() {
		connect();
		clickAndWait("$b1");
		clickAndWait("$b2");
		clickAndWait("$b3");
		clickAndWait("$b4");
		assertTrue(jq("$sb").exists(), "the selectbox should be rendered once the outer groupbox opens");
		assertTrue(jq("$sb").isVisible(), "the inner groupbox should be open");
		assertEquals(1, jq("$inner").find(".z-selectbox").length(),
				"the selectbox should be rendered exactly once");
		assertNoJSError();
	}

	private void clickAndWait(String id) {
		click(jq(id));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("the AU command failed on " + id + ": " + e.getAlertText());
		}
		assertFalse(hasError(), "the AU command failed on " + id + ": " + jq(".z-error").text());
	}
}
