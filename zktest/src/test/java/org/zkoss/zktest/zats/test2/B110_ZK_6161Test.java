/* B110_ZK_6161Test.java

        Purpose:

        Description:

        History:
                Thu Sep 24 15:18:55 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.zkoss.test.webdriver.WebDriverTestCase;

public class B110_ZK_6161Test extends WebDriverTestCase {

	private static final String BEGIN = "$drb .z-daterangebox-begin";

	private static final String END = "$drb .z-daterangebox-end";

	private static final String DATEBOX = "$db .z-datebox-input";

	private static final String NIGHTS_BEGIN = "$drbNights .z-daterangebox-begin";

	private static final String NIGHTS_END = "$drbNights .z-daterangebox-end";

	/** What `zul.db.Datebox` already says for the page's format, and what the
	 *  daterangebox has to say too. Before the fix it said "Invalid range". */
	private static final String FORMAT_MESSAGE = "You must specify a date. Format: yyyy/MM/dd";

	/** Types the text and blurs, then closes the calendar that focusing opened. */
	private void typeAndCommit(String selector, String text) {
		type(jq(selector), text);
		waitResponse();
		getActions().sendKeys(Keys.ESCAPE).perform();
		waitResponse();
	}

	/** The errorbox opens on a 50ms timer, so it is not up yet when waitResponse returns. */
	private String errorText() {
		new WebDriverWait(driver, Duration.ofSeconds(5))
				.until(d -> jq(".z-errorbox-content").exists());
		assertEquals(1, jq(".z-errorbox-content").length(),
				"exactly one errorbox may be open, or the text read back is ambiguous");
		return jq(".z-errorbox-content").text();
	}

	/** Unparseable text must name the pattern to type, not report a range problem. */
	@Test
	public void testUnparseableBeginNamesTheFormat() {
		connect();
		waitResponse();

		typeAndCommit(BEGIN, "abc");
		assertTrue(jq("$drb").hasClass("z-daterangebox-invalid"),
				"precondition: unparseable text must mark the box invalid");

		assertEquals(FORMAT_MESSAGE, errorText(),
				"a typo must be told the expected format; \"Invalid range\" belongs to the range rules");
	}

	/** The end input is served by the same branch and must say the same thing. */
	@Test
	public void testUnparseableEndNamesTheFormat() {
		connect();
		waitResponse();

		typeAndCommit(END, "abc");
		assertTrue(jq("$drb").hasClass("z-daterangebox-invalid"),
				"precondition: unparseable text must mark the box invalid");

		assertEquals(FORMAT_MESSAGE, errorText(),
				"the end input must name the format too, not only the begin input");
	}

	/** A listener reads the reason from the event, so the payload must carry it too. */
	@Test
	public void testOnErrorCarriesTheFormatMessage() {
		connect();
		waitResponse();

		typeAndCommit(BEGIN, "abc");

		assertEquals(FORMAT_MESSAGE, jq("$lblError").text(),
				"onError's message must be the text the user was shown");
	}

	/** The parity the ticket asks for: same format, same bad input, same sentence. */
	@Test
	public void testMatchesTheDateboxMessage() {
		connect();
		waitResponse();

		typeAndCommit(DATEBOX, "abc");
		String dateboxMessage = errorText();

		// Clear it so the next errorbox is the only one on screen.
		typeAndCommit(DATEBOX, "");
		new WebDriverWait(driver, Duration.ofSeconds(5))
				.until(d -> !jq(".z-errorbox").exists());

		typeAndCommit(BEGIN, "abc");

		assertEquals(dateboxMessage, errorText(),
				"the daterangebox must report an unparseable date the way the datebox does");
	}

	/** Out of scope for this ticket: a pair that parses keeps its own reason. */
	@Test
	public void testReversedRangeKeepsItsOwnMessage() {
		connect();
		waitResponse();

		typeAndCommit(NIGHTS_BEGIN, "2026/09/05");
		typeAndCommit(NIGHTS_END, "2026/09/01");

		assertEquals("Begin date must not be later than end date", errorText(),
				"a reversed range parses, so it must keep its own reason");
	}
}
