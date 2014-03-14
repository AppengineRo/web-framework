package ro.appenigne.web.framework.servlet;

import ro.appenigne.web.framework.annotation.UrlPattern;

@UrlPattern({"/_ah/start", "/_ah/stop"})
public class BackendNoOp extends AbstractIController {

    @Override
    public void execute() throws Exception {

    }
}
