<%--
B110-ZK-5725-SlowImage.jsp

	Purpose:

	Description:

	History:
		Thu Aug 20 15:20:00 CST 2026, Created by peakerlee

Copyright (C) 2026 Potix Corporation. All Rights Reserved.
--%><%@ page contentType="image/png" %><%
// An image that answers only after "d" seconds (2 by default) and then turns out not to be an
// image at all, so the browser reports it as loaded (with an error) only that late. It keeps
// zUtl.isImageLoading() true in the meantime. Used by B110-ZK-5725-ToolbarAdjust.zul.
String d = request.getParameter("d");
try {
	Thread.sleep(d == null ? 2000L : Long.parseLong(d) * 1000L);
} catch (InterruptedException ex) {
	Thread.currentThread().interrupt();
}
%>
