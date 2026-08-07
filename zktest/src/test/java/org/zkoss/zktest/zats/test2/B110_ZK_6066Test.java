/* B110_ZK_6066Test.java

        Purpose:
                
        Description:
                
        History:
                Fri Aug 07 12:41:35 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6066Test extends WebDriverTestCase {
	private String nodeAttr(String id, String sub, String attr) {
		return getEval("zk.Widget.$(jq('$" + id + "')[0]).$n('" + sub + "').getAttribute('" + attr + "')");
	}

	@Test
	public void switchMoldRoleHasNoExtraQuote() {
		connect();
		waitResponse();
		if (!Boolean.valueOf(getEval("!!window.za11y")))
			return;
		assertEquals("switch", nodeAttr("cbSwitch", "mold", "role"));
	}

	@Test
	public void switchMoldAriaStatesHaveNoExtraQuote() {
		connect();
		waitResponse();
		if (!Boolean.valueOf(getEval("!!window.za11y")))
			return;
		assertEquals("false", nodeAttr("cbSwitch", "mold", "aria-checked"));
		assertEquals("false", nodeAttr("cbSwitch", "mold", "aria-disabled"));
		assertEquals("true", nodeAttr("cbSwitchOn", "mold", "aria-checked"));
		assertEquals("true", nodeAttr("cbSwitchOn", "mold", "aria-disabled"));
	}

	@Test
	public void toggleMoldRoleHasNoExtraQuote() {
		connect();
		waitResponse();
		if (!Boolean.valueOf(getEval("!!window.za11y")))
			return;
		assertEquals("checkbox", nodeAttr("cbToggle", "mold", "role"));
		assertEquals("false", nodeAttr("cbToggle", "mold", "aria-checked"));
	}

	@Test
	public void defaultMoldKeepsRoleOnRealInput() {
		connect();
		waitResponse();
		if (!Boolean.valueOf(getEval("!!window.za11y")))
			return;
		assertEquals("null", nodeAttr("cbDefault", "mold", "role"));
		assertEquals("checkbox", nodeAttr("cbDefault", "real", "role"));
	}
}
