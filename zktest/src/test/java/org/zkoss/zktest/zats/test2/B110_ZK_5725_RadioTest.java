/* B110_ZK_5725_RadioTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 16:00:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * Checking a radio resets the look of the other radios of the same group, and a radio that is
 * not drawn yet has no node to reset.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_RadioTest extends WebDriverTestCase {

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
		// the ghost radio is bound but has no DOM node, which is what setChecked() has to survive
		assertEquals("true", getEval("!!zk.Widget.$('$ghost')"),
				"the render-deferred radio should still be a bound widget");
		assertEquals("false", getEval("String(!!zk.Widget.$('$ghost').$n())"),
				"the render-deferred radio should have no DOM node");

		click(jq("$check"));
		// ZK-5725 threw "Node  is not found!" while looping over the other radios of the group,
		// which aborts the whole AU response and pops a native retry alert
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("the AU command failed: " + e.getAlertText());
		}
		assertFalse(hasError(), "the AU command failed: " + jq(".z-error").text());

		assertEquals("true", getEval("String(zk.Widget.$('$b').$n('real').checked)"),
				"b should be checked");
		assertEquals("false", getEval("String(zk.Widget.$('$a').$n('real').checked)"),
				"a should be unchecked");
		assertEquals("true", getEval("String(jq(zk.Widget.$('$a').$n()).hasClass('z-radio-off'))"),
				"a should get the unchecked look back");
		assertEquals("true", getEval("String(jq(zk.Widget.$('$b').$n()).hasClass('z-radio-on'))"),
				"b should get the checked look");
		// the bookkeeping of the undrawn radio must still be updated
		assertEquals("false", getEval("String(!!zk.Widget.$('$ghost')._checked)"),
				"the render-deferred radio should be unchecked too");
		assertNoJSError();
	}
}
