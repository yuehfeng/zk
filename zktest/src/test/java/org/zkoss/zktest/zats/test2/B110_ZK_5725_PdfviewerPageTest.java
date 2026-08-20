/* B110_ZK_5725_PdfviewerPageTest.java

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

import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * The a11y page label of Pdfviewer is written from a promise continuation, which also runs for a
 * viewer that has been detached meanwhile, so it must not demand the -content node.
 *
 * @author peakerlee
 */
public class B110_ZK_5725_PdfviewerPageTest extends WebDriverTestCase {

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
		// the page label under test is added by the za11y layer. A silent return would make the NO_A11Y run
		// report a pass, so abort the test instead and let it show up as skipped.
		Assumptions.assumeTrue(Boolean.parseBoolean(getEval("!!window.za11y")),
				"the za11y layer is not loaded, the page label under test does not exist");
		waitResponse();

		// the first viewer detaches itself the moment its first page has been rendered, with page 2
		// left queued - ZK-5725 threw "Node with content is not found!" on that queued page
		assertTrue(waitFor("!!window.zk5725Detached"), "the first page was never rendered");
		sleep(1000);
		String box = getEval("String((jq('.z-error')[0] || {}).textContent || '')");
		assertFalse(hasError(), "an error box appeared: " + box);
		assertEquals("0", getEval("window.zk5725Errors.length"),
				"a JS error was thrown: " + getEval("window.zk5725Errors.join(' | ')"));
		assertEquals("0", getEval("window.zk5725Rejections.length"),
				"a promise was rejected: " + getEval("window.zk5725Rejections.join(' | ')"));

		// the label is still written for a viewer that is still there
		assertTrue(waitFor("!!(zk.Widget.$('$pvOk') && zk.Widget.$('$pvOk').$n('content')"
				+ " && zk.Widget.$('$pvOk').$n('content').getAttribute('aria-label'))"),
				"the second viewer never rendered a page");
		assertEquals("#1", getEval("String(zk.Widget.$('$pvOk').$n('content').getAttribute('aria-label'))"),
				"the a11y page label should still be written on a bound viewer");
		assertNoAnyError();
	}

	private boolean waitFor(String script) {
		for (int i = 0; i < 40; i++) {
			if (Boolean.parseBoolean(getEval(script)))
				return true;
			sleep(500);
		}
		return false;
	}
}
