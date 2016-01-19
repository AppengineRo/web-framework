package ro.appenigne.web.framework.servlet;

import ro.appenigne.web.framework.annotation.UrlPattern;

import javax.servlet.http.HttpServletResponse;

@UrlPattern({"/bower_components"})
public class NotFound extends AbstractIController {

    @Override
    public void execute() throws Exception {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "NotFound");
    }
}
