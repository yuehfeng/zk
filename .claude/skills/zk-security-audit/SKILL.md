---
name: zk-security-audit
description: Use when data reaches HTML, attribute, URL, JavaScript, resource routing, upload/download, or EE/CE boundary surfaces.
---

# ZK Security Audit

For security-sensitive server or client changes. Reading the touched
`.java`/`.ts`/`.js` files auto-loads `security-accessibility.md` (server encoder
→ sink table, resource routing, media, ARIA) — use it as the reference.

## Audit procedure

1. Trace each value from its source to its sink; confirm the encoder matches the
   sink (the wrong `Encode.forX` is still XSS). Client sinks (`innerHTML`,
   `document.write`, `location.href`) are already lint-rejected — focus on the
   Java side and on raw-by-design sinks (`Html#setContent`, mold `/*safe*/`).
2. Check URL query values are encoded per name/value — `encodeURL` does not touch
   anything after `?`.
3. Confirm CE has no compile-time EE dependency.
4. Check resource routes, upload/download filenames, and media streams for
   repeatability and centralized (servlet/extendlet/WPD) ownership.
5. Cite the framework precedent when a finding depends on a convention, so it is
   not mistaken for a known-correct pattern (see `zk-review` do-not-report list).
