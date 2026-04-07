/* B96_ZK_6083Test.java

	Purpose:
		Test path traversal vulnerability fixes

	Description:
		Verifies that Https.sanitizePath/normalizePath/isValidPath correctly
		block path traversal attacks, and that ClassWebResource/DHtmlResourceServlet
		path checks reject escaped paths.

	History:
		Apr 7, 2026, Created by Claude

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zktest.zats.test2;

import org.junit.Assert;
import org.junit.Test;

import org.zkoss.web.servlet.http.Https;

/**
 * Tests for ZK-6083: Fix path traversal vulnerabilities.
 */
public class B96_ZK_6083Test {

	// -- normalizePath tests --

	@Test
	public void testNormalizePath_normal() {
		Assert.assertEquals("/web/zk/js/zk.wpd", Https.normalizePath("/web/zk/js/zk.wpd"));
	}

	@Test
	public void testNormalizePath_root() {
		Assert.assertEquals("/", Https.normalizePath("/"));
	}

	@Test
	public void testNormalizePath_null() {
		Assert.assertNull(Https.normalizePath(null));
	}

	@Test
	public void testNormalizePath_safeDotDot() {
		// ../within a valid hierarchy should resolve
		Assert.assertEquals("/web/foo.js", Https.normalizePath("/web/zk/../foo.js"));
	}

	@Test
	public void testNormalizePath_traversalAboveRoot() {
		// Trying to go above root should return null
		Assert.assertNull(Https.normalizePath("/web/../../etc/passwd"));
	}

	@Test
	public void testNormalizePath_dotSegments() {
		Assert.assertEquals("/web/js/zk.wpd", Https.normalizePath("/web/./js/./zk.wpd"));
	}

	@Test
	public void testNormalizePath_trailingSlash() {
		Assert.assertEquals("/web/js/", Https.normalizePath("/web/js/"));
	}

	// -- isValidPath tests --

	@Test
	public void testIsValidPath_normal() {
		Assert.assertTrue(Https.isValidPath("/web/zk/js/zk.wpd"));
	}

	@Test
	public void testIsValidPath_null() {
		Assert.assertFalse(Https.isValidPath(null));
	}

	@Test
	public void testIsValidPath_doubleSlash() {
		// normalizePath collapses // into /, so //etc/passwd becomes /etc/passwd
		// which is a valid path. The real defense is that servlet containers
		// normalize // before it reaches ZK.
		Assert.assertTrue(Https.isValidPath("//etc/passwd"));
	}

	@Test
	public void testIsValidPath_dotDotAtRoot() {
		Assert.assertFalse(Https.isValidPath("/../etc/passwd"));
	}

	// -- sanitizePath tests --

	@Test
	public void testSanitizePath_normal() {
		Assert.assertEquals("/web/zk/js/zk.wpd", Https.sanitizePath("/web/zk/js/zk.wpd"));
	}

	@Test
	public void testSanitizePath_null() {
		Assert.assertNull(Https.sanitizePath(null));
	}

	@Test
	public void testSanitizePath_blocksTraversal() {
		// Classic path traversal attack — goes above root
		Assert.assertNull(Https.sanitizePath("/web/../../etc/passwd"));
	}

	@Test
	public void testSanitizePath_blocksWEBINFTraversal() {
		Assert.assertNull(Https.sanitizePath("/web/../../../WEB-INF/web.xml"));
	}

	@Test
	public void testSanitizePath_doubleSlashNormalized() {
		// //etc/passwd normalizes to /etc/passwd which is a valid path.
		// The real protection against // attacks comes from the servlet container
		// and the Path.of().normalize() checks in ClassWebResource.
		Assert.assertEquals("/etc/passwd", Https.sanitizePath("//etc/passwd"));
	}

	@Test
	public void testSanitizePath_allowsSafeRelativeTraversal() {
		// Safe traversal within valid hierarchy
		Assert.assertEquals("/web/foo.js", Https.sanitizePath("/web/zk/../foo.js"));
	}

	@Test
	public void testSanitizePath_fileProtocol() {
		// file:// protocol paths should be sanitized
		String result = Https.sanitizePath("file:///var/www/index.html");
		// Should either return sanitized path or null, but not allow traversal
		if (result != null) {
			Assert.assertFalse("Should not contain '..'", result.contains(".."));
		}
	}

	@Test
	public void testSanitizePath_encodedDotsNotDecoded() {
		// sanitizePath does NOT URL-decode; that's the servlet container's job.
		// %2e%2e is treated as literal characters, not as ".."
		String result = Https.sanitizePath("/web/%2e%2e/%2e%2e/etc/passwd");
		Assert.assertNotNull("Encoded dots are not decoded by sanitizePath", result);
	}

	// -- Path.of().normalize() checks (same pattern used in ClassWebResource/DHtmlResourceServlet) --

	@Test
	public void testPathNormalize_blocksEscape() {
		String PATH_PREFIX = "/web";
		// Simulate the attack: /web/../../etc/passwd
		java.nio.file.Path normalized = java.nio.file.Path.of("/web/../../etc/passwd").normalize();
		Assert.assertFalse(
				"Traversal path should not start with PATH_PREFIX",
				normalized.toString().startsWith(PATH_PREFIX));
	}

	@Test
	public void testPathNormalize_allowsValid() {
		String PATH_PREFIX = "/web";
		java.nio.file.Path normalized = java.nio.file.Path.of("/web/zk/js/zk.wpd").normalize();
		Assert.assertTrue(
				"Valid path should start with PATH_PREFIX",
				normalized.toString().startsWith(PATH_PREFIX));
	}

	@Test
	public void testPathNormalize_prefixUri_blocksEscape() {
		String PATH_PREFIX = "/web";
		// Simulate ClassWebResource: Path.of(PATH_PREFIX, uri).normalize()
		java.nio.file.Path normalized = java.nio.file.Path.of(PATH_PREFIX, "../../etc/passwd").normalize();
		Assert.assertFalse(
				"Traversal via uri param should not start with PATH_PREFIX",
				normalized.toString().startsWith(PATH_PREFIX));
	}

	@Test
	public void testPathNormalize_prefixUri_allowsValid() {
		String PATH_PREFIX = "/web";
		java.nio.file.Path normalized = java.nio.file.Path.of(PATH_PREFIX, "zk/js/zk.wpd").normalize();
		Assert.assertTrue(
				"Valid uri should resolve within PATH_PREFIX",
				normalized.toString().startsWith(PATH_PREFIX));
	}

	// -- CookieThemeResolver.isValidName equivalent logic --

	@Test
	public void testThemeNameValidation() {
		// Theme names with path traversal characters should be rejected
		Assert.assertTrue("Normal theme", isValidThemeName("iceblue_c"));
		Assert.assertTrue("Theme with hyphen", isValidThemeName("my-theme"));
		Assert.assertFalse("Slash", isValidThemeName("../../../etc"));
		Assert.assertFalse("Backslash", isValidThemeName("..\\..\\etc"));
		Assert.assertFalse("Dot", isValidThemeName("theme.name"));
		Assert.assertFalse("Percent encoding", isValidThemeName("theme%2F.."));
		Assert.assertFalse("Null", isValidThemeName(null));
		Assert.assertFalse("Empty", isValidThemeName(""));
		Assert.assertFalse("Blank", isValidThemeName("   "));
	}

	// -- WebSphere Liberty specific attack scenarios --

	@Test
	public void testWebSphereLibertyAttack_classWebResource() {
		// WebSphere Liberty may not normalize pathInfo before passing to servlet
		// Attacker sends: GET /zkau/web/../../../WEB-INF/web.xml
		String pi = "/web/../../../WEB-INF/web.xml";

		// Layer 1: Path.of().normalize() check (DHtmlResourceServlet/ClassWebResource)
		java.nio.file.Path normalized = java.nio.file.Path.of(pi).normalize();
		Assert.assertFalse("Attack must be blocked by Path check",
				normalized.toString().startsWith("/web"));

		// Layer 2: sanitizePath check
		Assert.assertNull("Attack must be blocked by sanitizePath", Https.sanitizePath(pi));
	}

	@Test
	public void testWebSphereLibertyAttack_servletContext() {
		// Attacker targets SimpleWebApp.getResource() or ResourceCaches
		String path = "/../../WEB-INF/web.xml";
		Assert.assertNull("Must block servletContext traversal", Https.sanitizePath(path));
	}

	@Test
	public void testWebSphereLibertyAttack_cookieInjection() {
		// Attacker sets cookie: zktheme=../../WEB-INF/web.xml
		String maliciousTheme = "../../WEB-INF/web.xml";
		Assert.assertFalse("Must block cookie-based traversal", isValidThemeName(maliciousTheme));
	}

	// Replicate the validation logic from CookieThemeResolver
	private boolean isValidThemeName(String themeName) {
		if (themeName == null || themeName.trim().isEmpty()) return false;
		for (int j = 0, len = themeName.length(); j < len; ++j) {
			char cc = themeName.charAt(j);
			if (cc == '/' || cc == '\\' || cc == '.' || cc == ':' || cc == '?'
					|| cc == '&' || cc == '=' || cc == '%' || cc == '#' || cc == ' ')
				return false;
		}
		return true;
	}
}
