package ro.appenigne.web.framework.servlet;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import ro.appenigne.web.framework.exception.UnauthorizedAccess;

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

            @Override
            public Entity getCont(String _hashContCurent) throws EntityNotFoundException, UnauthorizedAccess {
                return null;
            }

            @Override
            public void logAuthInfo() {
            }
        }.run(req, resp, null);
    }

    public abstract void execute(HttpServletRequest req, HttpServletResponse resp) throws Exception;

}
