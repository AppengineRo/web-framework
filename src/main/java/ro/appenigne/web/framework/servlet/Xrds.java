package ro.appenigne.web.framework.servlet;


import ro.appenigne.web.framework.annotation.UrlPattern;
import ro.appenigne.web.framework.utils.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;


/**
 * http://jeremiahlee.com/blog/2009/09/28/how-to-setup-openid-with-google-apps/
 * http://en.wikipedia.org/wiki/XRDS
 * https://developers.google.com/identity-toolkit/v2/devconsole
 * http://stackoverflow.com/questions/7529013/aol-openid-website-verification
 * Pe scurt yahoo + inca cateva vor sa verifice ceva legat de OpenID
 */
@UrlPattern("/_ah/xrds")
public class Xrds extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Log.echo(Level.CONFIG, "xrds");
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns:openid=\"http://openid.net/xmlns/1.0\" xmlns=\"xri://$xrd*($v*2.0)\">\r\n"
                + "	<XRD>\r\n"
                + "		<Service priority=\"1\">\r\n"
                + "			<Type>http://specs.openid.net/auth/2.0/return_to</Type>\r\n"
                + "			<URI>"
                + req.getRequestURL().substring(0, req.getRequestURL().indexOf("/", 8))
                + "/_ah/openid_verify</URI>\r\n"
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
