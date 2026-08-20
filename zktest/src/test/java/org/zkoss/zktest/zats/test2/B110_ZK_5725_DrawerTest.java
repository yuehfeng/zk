/* B110_ZK_5725_DrawerTest.java

	Purpose:

	Description:

	History:
		Thu Aug 20 21:00:00 CST 2026, Created by peakerlee

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
 * A drawer that is bound but not rendered yet must not demand its -real node when the server
 * opens it.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_DrawerTest extends WebDriverTestCase {

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

		// 1. the implicit dependency that keeps every drawer mount safe: the client default has to
		// mirror Drawer.java's setVisibleDirectly(false), otherwise the visible:false the server
		// sends at mount enters setVisible() while the drawer still has no DOM
		assertEquals("false", getEval("String(new zkmax.wgt.Drawer()._visible)"),
				"zkmax.wgt.Drawer must default _visible to false");

		// 2. the setup: the deferred drawer is a bound widget without a DOM node
		assertEquals("true", getEval("String(!!zk.Widget.$('$deferred'))"),
				"the deferred drawer should be a bound widget");
		assertEquals("false", getEval("String(!!zk.Widget.$('$deferred').desktop)"),
				"the deferred drawer should have no DOM node yet");

		// ZK-5725 threw "Node with real is not found!" here and aborted the AU response
		click(jq("$openDeferred"));
		try {
			waitResponse();
		} catch (UnhandledAlertException e) {
			fail("the AU response was aborted: " + e.getAlertText());
		}
		assertFalse(hasError(), "opening a deferred drawer should not raise an error box");
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		// the state is still recorded, so the deferred render will draw an open drawer
		assertEquals("true", getEval("String(zk.Widget.$('$deferred').isVisible())"),
				"the open state should still be recorded on the deferred drawer");

		// 3. a rendered drawer still opens - the control for the lifecycle assertions below. It is
		// done before the deferred one because an open drawer masks the page and would swallow
		// any later click.
		getEval("(window.zk5725Shown.length = 0, '')");
		click(jq("$openRendered"));
		waitResponse();
		assertTrue(jq("$rendered").hasClass("z-drawer-open"), "the rendered drawer should open");
		assertOpenLifecycle("rendered");

		// 4. the deferred render finally happens. Recording the state was only half the job: the
		// open lifecycle the missing DOM swallowed has to be completed now, or the drawer merely
		// looks open while it stays inside its parent and traps no keyboard.
		getEval("(window.zk5725Shown.length = 0, zk.Widget.$('$wrap').forcerender(), '')");
		waitResponse();
		assertTrue(jq("$deferred").exists(), "the deferred drawer should be rendered now");
		assertTrue(jq("$deferred").hasClass("z-drawer-open"), "the deferred drawer should look open");
		assertOpenLifecycle("deferred");

		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertNoAnyError();
	}

	/**
	 * An open drawer floats over the page, so it has to leave its parent, trap the keyboard and
	 * tell its content that it is on screen.
	 */
	private void assertOpenLifecycle(String id) {
		assertEquals("BODY", getEval("zk.Widget.$('$" + id + "').$n().parentNode.tagName"),
				"an open drawer should be moved out of its parent (makeVParent)");
		assertEquals("true", getEval("String(!!zk.Widget.$('$" + id + "')._trap)"),
				"an open drawer should trap the keyboard inside itself");
		assertTrue(Boolean.parseBoolean(getEval("String(window.zk5725Shown.indexOf('" + id + "') >= 0)")),
				"an open drawer should tell its content that it is on screen (fireShown)");
	}
}
