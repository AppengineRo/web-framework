package ro.appenigne.web.framework.auth;


import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.util.SocialAuthUtil;
import ro.appenigne.web.framework.annotation.UrlPattern;
import ro.appenigne.web.framework.utils.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;


@UrlPattern("/connect")
public class ConnectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SocialAuthManager authManager;
        AppEngineSession session = new AppEngineSession(req);
        if(session.getAttribute("authManager")!=null){
            authManager = (SocialAuthManager) session.getAttribute("authManager");
            if(authManager!=null){
                Map<String, String> requestParametersMap = SocialAuthUtil.getRequestParametersMap(req);
                try {
                    authManager.connect(requestParametersMap);
                    session.setAttribute("authManager", authManager, resp);
                    resp.sendRedirect(URLDecoder.decode(req.getParameter("redirect_to"), "UTF-8"));
                } catch (Exception e) {
                    Log.w(e);
                    resp.getWriter().print(e.getMessage());
                }
            }else{
                resp.getWriter().print("AuthManager is null");
            }
        }else{
            resp.getWriter().print("session attribute AuthManager is null");
        }


    }
}

