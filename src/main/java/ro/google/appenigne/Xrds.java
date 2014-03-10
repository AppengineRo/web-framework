package ro.google.appenigne;


import ro.google.appenigne.utils.Log;
import ro.google.appenigne.utils.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;

public class Xrds extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Log.echo(Level.CONFIG, "xrds");
		String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
			+ "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns:openid=\"http://openid.net/xmlns/1.0\" xmlns=\"xri://$xrd*($v*2.0)\">\r\n"
			+ "	<XRD>\r\n"
			+ "		<Service priority=\"1\">\r\n"
			+ "			<Type>http://specs.openid.net/auth/2.0/return_to</Type>\r\n"
			+ "			<URI>https://"
			+ Utils.getAppId()
			+ ".appspot.com</URI>\r\n"
			+ "		</Service>\r\n"
			+ "	</XRD>\r\n"
			+ "</xrds:XRDS>";

		resp.setContentType("application/xrds+xml");
		resp.getWriter().print(response);
		final long CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 30; // 30 days
		final long CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND * 1000L;
		long now = System.currentTimeMillis();
		resp.addHeader("Cache-Control", "max-age=" + CACHE_DURATION_IN_SECOND);
		resp.addHeader("Cache-Control", "must-revalidate");// optional
		resp.setDateHeader("Last-Modified", now);
		resp.setDateHeader("Expires", now + CACHE_DURATION_IN_MS);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		this.doGet(req, resp);
	}
    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doGet(req, resp);
    }
}
