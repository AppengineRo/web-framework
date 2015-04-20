package ro.appenigne.web.framework.auth;


import ro.appenigne.web.framework.annotation.UrlPattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;


@UrlPattern("/disconnect")
public class DisconnectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AppEngineSession session = new AppEngineSession(req);
        session.invalidate();
        resp.sendRedirect(URLDecoder.decode(req.getParameter("redirect_to"), "UTF-8"));
    }
}

