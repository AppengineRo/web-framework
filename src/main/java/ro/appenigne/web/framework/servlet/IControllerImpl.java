package ro.appenigne.web.framework.servlet;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class IControllerImpl extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        new AbstractIController() {
            @Override
            public void execute() throws Exception {
                IControllerImpl.this.execute(req, resp);
            }
        }.run(req, resp, null);
    }

    public abstract void execute(HttpServletRequest req, HttpServletResponse resp) throws Exception;

}
