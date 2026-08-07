/* B110_ZK_6067Test.java

        Purpose:
                
        Description:
                
        History:
                Fri Aug 07 14:52:05 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6067Test extends WebDriverTestCase {
	private int labelCount(String id) {
		return Integer.parseInt(
				getEval("document.getElementById(jq('$" + id + "')[0].id + '-real').labels.length"));
	}

	@Test
	public void defaultMoldHasOneLabel() {
		connect();
		waitResponse();
		assertEquals(1, labelCount("cbDefault"));
	}

	@Test
	public void tristateMoldHasOneLabel() {
		connect();
		waitResponse();
		assertEquals(1, labelCount("cbTristate"));
	}

	@Test
	public void contentLabelStillTogglesDefaultMold() {
		connect();
		waitResponse();
		assertEquals("false", getEval("!!zk.Widget.$(jq('$cbDefault')[0]).isChecked()"));

		click(jq("$cbDefault").find(".z-checkbox-content"));
		waitResponse();
		assertEquals("true", getEval("!!zk.Widget.$(jq('$cbDefault')[0]).isChecked()"),
				"demoting the mold node must not break the content label association");
	}

	/** The switch and toggle molds keep their label: it is the only thing that
	 * toggles them on click, so they still expose two labels.
	 */
	@Test
	public void switchAndToggleMoldsStillExposeTwoLabels() {
		connect();
		waitResponse();
		assertEquals(2, labelCount("cbSwitch"));
		assertEquals(2, labelCount("cbToggle"));
	}

	@Test
	public void clickingSwitchMoldToggles() {
		connect();
		waitResponse();
		clickMoldTogglesTest("cbSwitch");
	}

	@Test
	public void clickingToggleMoldToggles() {
		connect();
		waitResponse();
		clickMoldTogglesTest("cbToggle");
	}

	/** The mold node is the visible control for these molds, so clicking it
	 * must flip the checkbox.
	 */
	private void clickMoldTogglesTest(String id) {
		assertEquals("false", getEval("!!zk.Widget.$(jq('$" + id + "')[0]).isChecked()"));

		click(jq("$" + id).find(".z-checkbox-mold"));
		waitResponse();
		assertEquals("true", getEval("!!zk.Widget.$(jq('$" + id + "')[0]).isChecked()"),
				"clicking the " + id + " mold node must toggle the checkbox");
	}
}
