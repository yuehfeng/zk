/* F110_ZK_4927Test.java

	Purpose:

	Description:

	History:
		Wed Jun 24 14:58:47 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import org.zkoss.test.webdriver.WebDriverTestCase;

/**
 * ZK-4927: markdown editor (PE component {@code org.zkoss.zkex.zul.Mdeditor}). A
 * single {@code <mdeditor>} edits markdown (CodeMirror, reused from
 * {@code <codeeditor>}) with a formatting toolbar and a markdown-it+DOMPurify
 * preview; pure display is {@code mode="preview"}+{@code readonly}. Covers
 * render, preview (GFM + XSS), mode layout, the always-on full toolbar (count +
 * action), server-relative image-path resolution, keyboard, events,
 * readonly/disabled, colorScheme, dynamic reconfig, live preview, binding and
 * app-supplied ARIA.
 */
public class F110_ZK_4927Test extends WebDriverTestCase {
	private static final String PAGE = "/test2/F110-ZK-4927.zul";

	//-------- helpers --------//
	private String contentEditable(String id) {
		return getEval("jq('$" + id + " .cm-content')[0].contentEditable");
	}

	private String editorValue(String id) {
		return getEval("zk.Widget.$('$" + id + "').getValue()");
	}

	private int toolbarButtons(String id) {
		return Integer.parseInt(getEval(
				"String(jq('$" + id + "')[0].querySelectorAll('.z-mdeditor-toolbar-button').length)"));
	}

	private boolean hasModeClass(String id, String mode) {
		return "true".equals(getEval(
				"String(jq('$" + id + "')[0].classList.contains('z-mdeditor-" + mode + "'))"));
	}

	/** Native querySelector against an element — ZK jq's {@code $id descendant}
	 * selector is unreliable for multi-level selectors (see F110_ZK_6086CodeeditorTest). */
	private boolean has(String id, String selector) {
		return "true".equals(getEval(
				"String(!!jq('$" + id + "')[0].querySelector('" + selector + "'))"));
	}

	/** The toolbar button carrying the given icon. Selected by icon, not by label:
	 * za11y localizes the button's aria-label/title from msgza11y. */
	private WebElement toolbarButton(String id, String icon) {
		return (WebElement) ((JavascriptExecutor) driver).executeScript(
				"return jq('$" + id + "')[0].querySelector('.z-icon-" + icon + "').closest('button')");
	}

	private String previewHtml(String id) {
		return getEval("jq('$" + id + "')[0].querySelector('.z-mdeditor-preview-pane').innerHTML");
	}

	private String paneDisplay(String id, String sub) {
		return getEval(
				"getComputedStyle(jq('$" + id + "')[0].querySelector('.z-mdeditor-" + sub + "')).display");
	}

	/** The modifier the widget accepts for bold/italic: Cmd on macOS, Ctrl elsewhere.
	 * Read from `zk.mac` so the test tracks the widget instead of guessing the host. */
	private Keys boldModifier() {
		return "true".equals(getEval("String(!!zk.mac)")) ? Keys.COMMAND : Keys.CONTROL;
	}

	/** The modifier the widget must ignore — on macOS Ctrl-b/Ctrl-i are CodeMirror's
	 * caret moves, so accepting them would move the caret and then wrap. */
	private Keys ignoredModifier() {
		return boldModifier() == Keys.COMMAND ? Keys.CONTROL : Keys.COMMAND;
	}

	private void typeInto(String id, CharSequence text) {
		getActions().moveToElement(toElement(jq("$" + id + " .cm-content"))).click().sendKeys(text).perform();
	}

	// ===== render: editor mounts (PE Runtime.init passed) + preview renders =====
	@Test
	public void editorMountsAndPreviewRenders() {
		connect(PAGE);
		waitResponse();
		assertTrue(jq(".z-mdeditor").exists(), "mdeditor wrapper missing");
		assertTrue(jq("$mdEditor .cm-editor").exists(), "CodeMirror not mounted inside mdeditor");
		// the preview-mode instance renders the source on the client
		assertTrue(has("mdView", "h1"), "h1 not rendered by markdown-it");
		assertTrue(has("mdView", "pre code"), "fenced code block not rendered");
		assertTrue(has("mdView", "table"), "GFM table not rendered");
	}

	// ===== preview: markdown features =====
	@Test
	public void previewRendersMarkdownElements() {
		connect(PAGE);
		waitResponse();
		assertTrue(has("mdView", "h2"), "h2");
		assertTrue(has("mdView", "strong"), "bold -> strong");
		assertTrue(has("mdView", "em"), "italic -> em");
		assertTrue(has("mdView", "s"), "strikethrough -> s");
		assertTrue(has("mdView", "blockquote"), "blockquote");
		assertTrue(has("mdView", "ul li"), "unordered list");
		assertTrue(has("mdView", "ol li"), "ordered list");
		assertTrue(has("mdView", "a"), "link -> a");
		assertTrue(has("mdView", "img"), "image -> img");
		assertTrue(has("mdView", "li > label > input[type=checkbox]:not([checked])"), "open task -> unchecked box");
		assertTrue(has("mdView", "li > label > input[type=checkbox][checked]"), "done task -> checked box");
	}

	// ===== raw HTML is escaped (preview pane only; the editor holds raw source as text) =====
	@Test
	public void rawHtmlEscaped() {
		connect(PAGE);
		waitResponse();
		assertFalse(has("mdView", "script"), "<script> must not become an element");
		assertFalse(has("mdView", "[onerror]"), "inline event handler must not survive");
		assertFalse(has("mdView", "a[href^=javascript]"), "javascript: URLs must not survive");
		assertFalse(has("mdView", "iframe"), "<iframe> must not become an element");
		assertFalse(has("mdView", ".z-mdeditor-preview-pane style, .z-mdeditor-preview-pane [style]"), "<style> and style= must not restyle the page");
		assertTrue(previewHtml("mdView").contains("&lt;style&gt;"), "raw HTML is shown as text");
	}

	// ===== mode: editor survives switch + correct modifier class =====
	@Test
	public void modeSwitchKeepsEditor() {
		connect(PAGE);
		waitResponse();
		for (String mode : new String[] {"source", "preview", "split"}) {
			click(jq("$mode" + mode.substring(0, 1).toUpperCase() + mode.substring(1)));
			waitResponse();
			assertTrue(jq("$mdEditor .cm-editor").exists(), "CodeMirror missing after mode=" + mode);
			assertTrue(hasModeClass("mdEditor", mode), "root missing z-mdeditor-" + mode);
		}
	}

	// ===== mode: pane layout =====
	@Test
	public void modeLayoutShowsHidesPanes() {
		connect(PAGE);
		waitResponse();
		click(jq("$modeSource"));
		waitResponse();
		assertEquals("none", paneDisplay("mdEditor", "preview-pane"), "source: preview hidden");
		assertNotEquals("none", paneDisplay("mdEditor", "cave"), "source: editor visible");
		click(jq("$modePreview"));
		waitResponse();
		assertEquals("none", paneDisplay("mdEditor", "cave"), "preview: editor hidden");
		assertNotEquals("none", paneDisplay("mdEditor", "preview-pane"), "preview: preview visible");
		click(jq("$modeSplit"));
		waitResponse();
		assertNotEquals("none", paneDisplay("mdEditor", "cave"), "split: editor visible");
		assertNotEquals("none", paneDisplay("mdEditor", "preview-pane"), "split: preview visible");
	}

	// ===== toolbar: the full set is always shown =====
	@Test
	public void toolbarAlwaysFull() {
		connect(PAGE);
		waitResponse();
		assertEquals(14, toolbarButtons("mdEditor"), "the full toolbar is always shown");
	}

	// ===== image: server-relative path resolved against the webapp context =====
	@Test
	public void imageSrcResolvedToBackendPath() {
		connect(PAGE);
		waitResponse();
		// the source has ![alt](/test2/img/sun.jpg); the preview rewrites the
		// <img> src through zk.ajaxURI, prepending the desktop context path so it
		// points at the backend resource (mirrors server-side Executions.encodeURL).
		String src = getEval("jq('$mdView')[0].querySelector('img').getAttribute('src')");
		assertTrue(src.endsWith("/test2/img/sun.jpg"), "resolved src keeps the resource path: " + src);
		assertNotEquals("/test2/img/sun.jpg", src, "context path was prepended (not the bare app-relative path)");
		assertEquals(getEval("zk.ajaxURI('/test2/img/sun.jpg')"), src, "img src resolved exactly as zk.ajaxURI would");
	}

	// ===== toolbar: a button mutates the editor =====
	@Test
	public void toolbarBoldButtonInsertsMarkers() {
		connect(PAGE);
		waitResponse();
		getActions().moveToElement(toolbarButton("mbTb", "bold")).click().perform();
		waitResponse();
		assertEquals("****", editorValue("mbTb"), "Bold button inserted the bold markers");
	}

	// ===== keyboard: Ctrl/Cmd+B =====
	@Test
	public void boldKeyboardShortcut() {
		connect(PAGE);
		waitResponse();
		Keys mod = boldModifier();
		getActions().moveToElement(toElement(jq("$mbType .cm-content"))).click()
				.keyDown(mod).sendKeys("b").keyUp(mod).perform();
		waitResponse();
		assertEquals("****", editorValue("mbType"), mod + "+B inserted bold markers");
	}

	@Test
	public void boldShortcutIgnoresTheOtherModifier() {
		connect(PAGE);
		waitResponse();
		Keys mod = ignoredModifier();
		getActions().moveToElement(toElement(jq("$mbType .cm-content"))).click()
				.keyDown(mod).sendKeys("b").keyUp(mod).perform();
		waitResponse();
		assertEquals("", editorValue("mbType"), mod + "+B must not wrap (it is CodeMirror's own binding)");
	}

	// ===== keyboard: Ctrl/Cmd+I wraps at the caret, not CodeMirror's Mod-i selectParentSyntax =====
	@Test
	public void italicShortcutWrapsAtCaret() {
		connect(PAGE);
		waitResponse();
		Keys mod = boldModifier();
		typeInto("mbType", "one two");
		getActions().sendKeys(Keys.LEFT, Keys.LEFT).keyDown(mod).sendKeys("i").keyUp(mod).perform();
		waitResponse();
		assertEquals("one t**wo", editorValue("mbType"), mod + "+I inserted the markers at the caret");
	}

	// ===== toolbar: a line prefix applies to every selected line; a heading does not stack =====
	@Test
	public void prefixAppliesToEverySelectedLine() {
		connect(PAGE);
		waitResponse();
		typeInto("mbTb", "a" + Keys.ENTER + "b");
		getEval("(zk.Widget.$('$mbTb')._cm.dispatch({selection: {anchor: 0, head: 3}}), 'done')");
		getActions().moveToElement(toolbarButton("mbTb", "list-ul")).click().perform();
		waitResponse();
		assertEquals("- a\n- b", editorValue("mbTb"), "both selected lines got the list marker");
	}

	@Test
	public void headingReplacesExistingMarker() {
		connect(PAGE);
		waitResponse();
		typeInto("mbTb", "Title");
		WebElement heading = toolbarButton("mbTb", "header");
		getActions().moveToElement(heading).click().click().perform();
		waitResponse();
		assertEquals("## Title", editorValue("mbTb"), "a second Heading click does not stack markers");
	}

	// ===== toolbar: a block (fence, rule, table) sits on lines of its own =====
	@Test
	public void codeBlockFencesOnOwnLines() {
		connect(PAGE);
		waitResponse();
		typeInto("mbTb", "Before code after");
		getEval("(zk.Widget.$('$mbTb')._cm.dispatch({selection: {anchor: 7, head: 11}}), 'done')");
		getActions().moveToElement(toolbarButton("mbTb", "file-code")).click().perform();
		waitResponse();
		assertEquals("Before \n\n```\ncode\n```\n\n after", editorValue("mbTb"), "the fences do not share a line with the prose");
	}

	@Test
	public void horizontalRuleAfterParagraph() {
		connect(PAGE);
		waitResponse();
		typeInto("mbTb", "Paragraph");
		getActions().moveToElement(toolbarButton("mbTb", "minus")).click().perform();
		waitResponse();
		assertEquals("Paragraph\n\n---", editorValue("mbTb"), "a blank line keeps the rule from turning the paragraph into a heading");
	}

	@Test
	public void tableKeepsFollowingTextOut() {
		connect(PAGE);
		waitResponse();
		typeInto("mbTb", "x" + Keys.ENTER + "y");
		getEval("(zk.Widget.$('$mbTb')._cm.dispatch({selection: {anchor: 1}}), 'done')");
		getActions().moveToElement(toolbarButton("mbTb", "table")).click().perform();
		waitResponse();
		assertEquals("x\n\n| Column | Column |\n| --- | --- |\n| Cell | Cell |\n\ny", editorValue("mbTb"),
				"a blank line keeps the next line from becoming a table row");
	}

	// ===== WCAG: names, keyboard access, focus ring, contrast =====
	@Test
	public void taskCheckboxIsLabelledByItemText() {
		connect(PAGE);
		waitResponse();
		assertEquals("open task", getEval("jq('$mdView')[0].querySelector('li > label > input:not([checked])').parentNode.textContent.trim()"),
				"the disabled checkbox gets its accessible name from the item text");
	}

	@Test
	public void previewPaneIsKeyboardScrollable() {
		connect(PAGE);
		waitResponse();
		assertEquals("0", getEval("String(jq('$mdView')[0].querySelector('.z-mdeditor-preview-pane').tabIndex)"),
				"the scrollable preview is reachable by keyboard");
	}

	@Test
	public void toolbarFocusRingIsVisible() {
		connect(PAGE);
		waitResponse();
		WebElement bold = toolbarButton("mbTb", "bold");
		// no pointer interaction yet, so a scripted focus matches :focus-visible as a keyboard one does
		assertEquals(true, ((JavascriptExecutor) driver).executeScript("arguments[0].focus(); return arguments[0].matches(':focus-visible')", bold));
		assertEquals("solid", ((JavascriptExecutor) driver).executeScript("return getComputedStyle(arguments[0]).outlineStyle", bold));
		assertNotEquals("rgba(0, 0, 0, 0)", ((JavascriptExecutor) driver).executeScript("return getComputedStyle(arguments[0]).outlineColor", bold),
				"the default theme's transparent active border must not be the ring colour");
	}

	@Test
	public void darkPreviewLinkKeepsContrast() {
		connect(PAGE);
		waitResponse();
		assertEquals("rgb(86, 156, 214)", getEval("getComputedStyle(jq('$mbDarkLink')[0].querySelector('.z-mdeditor-preview-pane a')).color"),
				"a dark preview link uses the dark palette, not the browser's #0000EE (1.8:1)");
	}

	// ===== za11y exports the toolbar labels to the client =====
	@Test
	public void toolbarMessagesExported() {
		connect(PAGE);
		waitResponse();
		assertEquals("Bold", getEval("String(msgza11y.MDEDITOR_BOLD)"), "MDEDITOR_* reaches msgza11y");
		assertEquals("Horizontal rule", getEval("String(msgza11y.MDEDITOR_HORIZONTAL_RULE)"));
	}

	// ===== setSclass rewrites className: the dark/disabled modifiers must survive =====
	@Test
	public void sclassKeepsDarkAndDisabled() {
		connect(PAGE);
		waitResponse();
		click(jq("$sclassBtn"));
		waitResponse();
		assertTrue(jq("$mbDark").hasClass("z-mdeditor-dark"), "dark modifier kept");
		assertTrue(jq("$mbDisabled").hasClass("z-mdeditor-disabled"), "disabled modifier kept");
		assertTrue(jq("$mbDark").hasClass("x"), "sclass applied");
	}

	// ===== events: focus / blur =====
	@Test
	public void focusAndBlurFireEvents() {
		connect(PAGE);
		waitResponse();
		getActions().moveToElement(toElement(jq("$mbFocus .cm-content"))).click().perform();
		waitResponse();
		assertEquals("focused", jq("$focusState").text(), "onFocus fired");
		getActions().moveToElement(toElement(jq("$mbOther .cm-content"))).click().perform();
		waitResponse();
		assertEquals("blurred", jq("$focusState").text(), "onBlur fired");
	}

	// ===== events: onChanging / onChange =====
	@Test
	public void typingFiresOnChanging() {
		connect(PAGE);
		waitResponse();
		typeInto("mbType", "abc");
		waitResponse();
		assertTrue(jq("$lblChanging").text().contains("abc"), "onChanging carried the typed text");
	}

	@Test
	public void blurFiresOnChange() {
		connect(PAGE);
		waitResponse();
		typeInto("mbType", "commitme");
		getActions().moveToElement(toElement(jq("$mbOther .cm-content"))).click().perform();
		waitResponse();
		assertTrue(jq("$lblChange").text().contains("commitme"), "onChange committed on blur");
	}

	// ===== readonly / disabled =====
	@Test
	public void readonlyNotEditable() {
		connect(PAGE);
		waitResponse();
		assertTrue(jq("$mbReadonly .cm-editor").exists(), "editor mounted");
		assertEquals("false", contentEditable("mbReadonly"), "readonly editor is not editable");
	}

	@Test
	public void disabledNotEditableAndDimmed() {
		connect(PAGE);
		waitResponse();
		assertEquals("false", contentEditable("mbDisabled"), "disabled editor is not editable");
		assertTrue("true".equals(getEval(
				"String(jq('$mbDisabled')[0].classList.contains('z-mdeditor-disabled'))")),
				"disabled editor carries the dimmed modifier class");
	}

	// ===== readonly / disabled: the toolbar must not mutate the document =====
	@Test
	public void toolbarDisabledWhenLocked() {
		connect(PAGE);
		waitResponse();
		for (String id : new String[] { "mbReadonly", "mbDisabled" })
			assertFalse(toolbarButton(id, "bold").isEnabled(), id + ": toolbar button must be disabled");
	}

	// the editor is hidden in preview mode, so a toolbar edit could never commit an onChange
	@Test
	public void toolbarDisabledInPreviewMode() {
		connect(PAGE);
		waitResponse();
		click(jq("$modePreview"));
		waitResponse();
		assertFalse(toolbarButton("mdEditor", "bold").isEnabled(), "preview: toolbar button must be disabled");
		click(jq("$modeSplit"));
		waitResponse();
		assertTrue(toolbarButton("mdEditor", "bold").isEnabled(), "split: toolbar button enabled again");
	}

	@Test
	public void toolbarCannotMutateLockedEditor() {
		connect(PAGE);
		waitResponse();
		// CodeMirror's EditorState.readOnly is consulted by *commands* only, so a
		// direct dispatch still applies — the widget has to refuse it itself. Call
		// the mutator directly; the disabled button above is only the outer belt.
		for (String id : new String[] { "mbReadonly", "mbDisabled" }) {
			String before = editorValue(id);
			getEval("(zk.Widget.$('$" + id + "')._wrap('**','**'), 'done')");
			waitResponse();
			assertEquals(before, editorValue(id), id + ": _wrap must not mutate a locked editor");
		}
	}

	// ===== colorScheme =====
	@Test
	public void darkThemeDiffersFromLight() {
		connect(PAGE);
		waitResponse();
		String dark = getEval("getComputedStyle(jq('$mbDark .cm-editor')[0]).backgroundColor");
		String light = getEval("getComputedStyle(jq('$mbLight .cm-editor')[0]).backgroundColor");
		assertNotEquals(light, dark, "dark theme renders a different editor background");
	}

	// ===== dynamic reconfiguration =====
	@Test
	public void dynamicModeToggle() {
		connect(PAGE);
		waitResponse();
		assertTrue(hasModeClass("mbDyn", "source"));
		click(jq("$dynMode"));
		waitResponse();
		assertTrue(hasModeClass("mbDyn", "split"), "setMode(split) applied live");
	}

	@Test
	public void dynamicReadonlyToggle() {
		connect(PAGE);
		waitResponse();
		assertEquals("true", contentEditable("mbDyn"));
		click(jq("$dynRO"));
		waitResponse();
		assertEquals("false", contentEditable("mbDyn"), "setReadonly(true) reconfigured the live editor");
	}

	@Test
	public void programmaticSetValue() {
		connect(PAGE);
		waitResponse();
		click(jq("$dynVal"));
		waitResponse();
		assertEquals("# changed by server", editorValue("mbDyn"), "server setValue pushed to the editor");
	}

	// ===== live preview =====
	@Test
	public void previewUpdatesOnEdit() {
		connect(PAGE);
		waitResponse();
		typeInto("mdEditor", "ZZUNIQUE ");
		waitResponse();
		assertTrue(previewHtml("mdEditor").contains("ZZUNIQUE"), "preview pane re-rendered with the new text");
	}

	// ===== two-way binding =====
	@Test
	public void twoWayBindingRoundTrips() {
		connect(PAGE);
		waitResponse();
		typeInto("mbBind", "BOUNDEDIT ");
		getActions().moveToElement(toElement(jq("$mbFocus .cm-content"))).click().perform();
		waitResponse();
		assertTrue(previewHtml("mdBound").contains("BOUNDEDIT"),
				"mdeditor onChange saved to the VM and the bound preview reloaded");
	}

	// ===== app-supplied ARIA =====
	@Test
	public void appAriaLabelForwardedToEditor() {
		connect(PAGE);
		waitResponse();
		assertEquals("my markdown editor",
				getEval("jq('$mbAria .cm-content')[0].getAttribute('aria-label')"),
				"ca:aria-label is forwarded onto CodeMirror's editable content");
	}
}
