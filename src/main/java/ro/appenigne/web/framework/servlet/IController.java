package ro.appenigne.web.framework.servlet;


import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import ro.appenigne.web.framework.utils.Datastore;
import ro.appenigne.web.framework.utils.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutionException;

/**
 * The public abstract class that all controllers must implement.
 */
public abstract class IController {

    public HttpServletRequest req;
    public HttpServletResponse resp;
    public Entity contCurent;
    public Datastore datastore;
    private String currentEmail;

    public final IController run(HttpServletRequest req, HttpServletResponse resp, Entity contCurent) throws Exception {
        this.req = req;
        this.resp = resp;
        this.contCurent = contCurent;
        this.datastore = new Datastore();
        execute();
        finish();
        return this;
    }

    public String getCurrentEmail(HttpServletRequest req) {
        if (this.currentEmail == null) {
            UserService userService = UserServiceFactory.getUserService();
            User user = userService.getCurrentUser();
            if (Utils.isTask(req)) {
                this.currentEmail = req.getParameter("_emailContCurent");
            } else if (user != null) {
                this.currentEmail = user.getEmail();
            }
        }
        return this.currentEmail;
    }

    /**
     * The execute method of the controllers.
     * Executes their specific action using the given HTTP request and response objects.
     */
    public abstract void execute() throws Exception;

    private void finish() throws ExecutionException, InterruptedException {
        datastore.commit();
    }
}
