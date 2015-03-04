package ro.appenigne.web.framework.servlet;


import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import ro.appenigne.web.framework.annotation.UrlPattern;
import ro.appenigne.web.framework.utils.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@UrlPattern("/GetMajorVersion")
public class DefaultVersion extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ModulesService modulesService = ModulesServiceFactory.getModulesService();
        String defaultVersion = modulesService.getDefaultVersion(req.getParameter("module"));
        resp.setContentType("text/plain");
        resp.getWriter().print(defaultVersion);
    }
}
