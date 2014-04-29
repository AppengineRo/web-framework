package ro.appenigne.web.framework.servlet;


import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import ro.appenigne.web.framework.annotation.UrlPattern;
import ro.appenigne.web.framework.utils.Log;

import java.io.IOException;


@UrlPattern("/getMajorVersion/{module}/")
public class DefaultVersion extends AbstractIController {

    @Override
    public void execute() throws IOException {
        ModulesService modulesService = ModulesServiceFactory.getModulesService();
        String defaultVersion = modulesService.getDefaultVersion(req.getParameter("module"));
        resp.setContentType("text/plain");
        resp.getWriter().print(defaultVersion);
    }
}
