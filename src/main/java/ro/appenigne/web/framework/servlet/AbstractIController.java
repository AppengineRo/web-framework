package ro.appenigne.web.framework.servlet;


import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.DeadlineExceededException;
import org.brickred.socialauth.Profile;
import ro.appenigne.web.framework.annotation.RequiredLogIn;
import ro.appenigne.web.framework.annotation.RequiredType;
import ro.appenigne.web.framework.annotation.XssCheck;
import ro.appenigne.web.framework.auth.AuthService;
import ro.appenigne.web.framework.datastore.Datastore;
import ro.appenigne.web.framework.datastore.KeysOnlyDatastoreCallback;
import ro.appenigne.web.framework.exception.InvalidAuthCookie;
import ro.appenigne.web.framework.exception.InvalidField;
import ro.appenigne.web.framework.exception.SendRedirect;
import ro.appenigne.web.framework.exception.UnauthorizedAccess;
import ro.appenigne.web.framework.request.FilteredRequest;
import ro.appenigne.web.framework.request.IgnoringArrayLikeParamNameRequest;
import ro.appenigne.web.framework.request.TrimRequest;
import ro.appenigne.web.framework.utils.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * The public abstract class that all controllers must implement.
 */
public abstract class AbstractIController {
    public String _hashContCurentParamName = "_hashContCurent";
    public String _hashContMasterSudoParamName = "_hashContMasterSudo";
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public Entity contCurent = null;
    public Datastore datastore;
    private String currentEmail;
    public Profile currentUser;
    public AuthService userService;

    public void preInit() throws SendRedirect {
        String reqUrl = req.getRequestURL().toString();
        String appDomain = Utils.getAppId() + ".appspot.com";

        if (!Utils.isTask(req) && (reqUrl.startsWith("http://" + appDomain) || reqUrl.startsWith("https://" + appDomain))) {
            if (req.getHeader("X-Requested-With") == null || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
                String domain = System.getProperty("domeniu");
                if (domain != null && !domain.isEmpty()) {
                    reqUrl = reqUrl.replace(appDomain, domain);
                    sendRedirect(reqUrl);
                }
            }
        }

        if (!req.isSecure() && !Utils.isLocalServer()) {
            if (reqUrl.contains(appDomain)) {
                String checkVersionChar = reqUrl.substring(0, reqUrl.indexOf(appDomain));
                if (checkVersionChar.contains(".")) {
                    reqUrl = reqUrl.replace(checkVersionChar, checkVersionChar.replaceAll("[\\.]+", "-dot-"));
                }

            }
            sendRedirect(reqUrl);
        }
    }

    public void run(HttpServletRequest req, HttpServletResponse resp, Entity contCurent) {
        try {
            initHttp(req, resp);
            preInit();
            setContCurent(contCurent);
            this.userService = new AuthService(req, resp);
            this.currentUser = userService.getCurrentUser();
            getCurrentEmail();
            createDatastore();
            createDatastoreCallbacks();
            String success = preExecute();
            finish();
            if(success.equals("Success")){
                onSuccess();
            } else if(success.equals( "Error")) {
                onError();
            } else {
                onWarning();
            }
        } catch (SendRedirect sendRedirect) {
            try {
                resp.sendRedirect(sendRedirect.getMessage());
            } catch (IOException e) {
                Log.s(e);
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            Log.s(e);
        }
    }

    public void onSuccess() {

    }

    public void onError() {

    }
    public void onWarning() {

    }
    public void setContCurent(Entity contCurent){
        if (this.contCurent == null || contCurent != null) {
            this.contCurent = contCurent;
        }
    }

    public void initHttp(HttpServletRequest req, HttpServletResponse resp) {
        initHttpReq(req);
        initHttpResp(resp);
    }

    /**
     * By default checks the annotation XssCheck and creates a filtered request of<br />
     * a TrimmedRequest and IgnoringArrayLikeParamNameRequest
     */
    public void initHttpReq(HttpServletRequest req) {
        XssCheck xssCheck = this.getClass().getAnnotation(XssCheck.class);
        this.req = new TrimRequest(new IgnoringArrayLikeParamNameRequest(req));
        if (xssCheck == null || xssCheck.value()) {
            this.req = new FilteredRequest(this.req);
        }
    }

    public void initHttpResp(HttpServletResponse resp) {
        this.resp = resp;
    }

    /**
     * Default implementation adds a datastore callback that prevents puts on empty entities
     */
    public void createDatastoreCallbacks() {
        datastore.addPrePut(new KeysOnlyDatastoreCallback());
    }

    public void createDatastore() {
        this.datastore = new Datastore(this.req);
    }

    @SuppressWarnings("unused")
    public String getCurrentEmail() {

        if (this.currentEmail == null) {
            if (Utils.isTask(req)) {
                this.currentEmail = req.getParameter("_emailContCurent");
            } else if (currentUser != null) {
                this.currentEmail = currentUser.getEmail();
            }
        }
        return this.currentEmail;
    }

    /**
     * The execute method of the controllers.
     * Executes their specific action using the given HTTP request and response objects.
     */
    public abstract void execute() throws Exception;

    private void finish() throws ExecutionException, InterruptedException, IOException {
        datastore.commit();
        /*try {
            resp.getWriter().flush();
        } catch(Exception e){
            Log.d(e);
        }
        try {
            resp.getOutputStream().flush();
        } catch(Exception e){
            Log.d(e);
        }
        Log.s("a trecut pe aici");*/
    }

    public String preExecute() {
        String queryLink = (req.getQueryString() != null) ? "/?" + req.getQueryString() : "/";
        try {
            try {
                setContCurent(getCont(req.getParameter(_hashContCurentParamName)));
                RequiredLogIn reqLogIn = this.getClass().getAnnotation(RequiredLogIn.class);

                if (reqLogIn != null && reqLogIn.value()) {
                    this.checkRole(this, contCurent);
                }
                // we don't need to check the backend version since we are using modules now (and dispatch.xml )
                // and we have no more problems with double authentication
                if (/*!Utils.isAppVersion("backend") && */!uniqueInstanceCookie()) {
                    throw new InvalidAuthCookie("uniqueInstanceCookie");
                }
                this.logAuthInfo();
                execute();
                if ("default".equals(Utils.getAppModule())) {
                    resp.setHeader("appV", SystemProperty.applicationVersion.get());
                }
                return "Success";
            } catch (UnauthorizedAccess | InvalidAuthCookie e) {
                Log.c(e);
                if (!isAjax()) {
                    resp.sendRedirect(userService.createLogoutURL(queryLink));
                } else {
                    resp.reset();
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().print(e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                return "Error";
            } catch (InvalidField e) {
                Log.c(e);
                if (Utils.isTask(req)) {
                    BlobKey blobKey = null;
                    if (req.getParameter("_blobKey") != null) {
                        blobKey = new BlobKey(req.getParameter("_blobKey"));
                    }
                    Utils.importFeedback(blobKey,
                            e.getMessage(), req);
                    return "Warning";
                } else {
                    //resp.reset();
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().print(e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return "Warning";
                }
            } catch (EntityNotFoundException | ApiProxy.CapabilityDisabledException e) {
                Log.s(e);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "Error";
            } catch (IllegalStateException e) {
                Log.s(e);
                this.preExecute();
            } catch (ApiProxy.OverQuotaException e) {
                Log.w(e);
            } catch (DeadlineExceededException e) {
                Log.s(e);
                resp.reset();
                resp.getWriter().print("admin.backend.deadlineExceededException");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "Error";
            } catch (OutOfMemoryError e) {
                Log.s(e);
                resp.reset();
                if (this.getClass().getSimpleName().equalsIgnoreCase("search")) {
                    resp.getWriter().print("admin.backend.outOfSearchMemoryError");
                } else {
                    resp.getWriter().print("admin.backend.outOfMemoryError");
                }
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "Error";
            } catch (DatastoreFailureException e) {
                Log.s(e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "Error";
            } catch (Exception e) {
                Log.s(e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return "Error";
            }
        } catch (Exception e) {
            Log.s(e);
        }
        return "Error";
    }

    public Entity getCont(String _hashContCurent) throws EntityNotFoundException, UnauthorizedAccess {
        Key keyCont;
        try {
            keyCont = KeyFactory.stringToKey(_hashContCurent);
        } catch (NullPointerException npe) {
            //Log.d(npe);
            return null;
        } catch (IllegalArgumentException iae) {
            //Log.d(iae);
            return null;
        }

        if (keyCont.getName() != null) {
            if (keyCont.getName().equals(AbstractUserType.SuperAdministrator)) {
                return getSuperAdminCont(req);
            }
        } else {
            Entity cont = datastore.getFromMemOrDb(_hashContCurent);
            Entity contMasterSudo = datastore.getFromMemOrDb(req.getParameter("_hashContMasterSudo"));
            if (currentUser == null) {
                if (Utils.isTask(req)) {
                    return cont;
                } else {
                    return null;
                }
            }
            if (!userService.isUserAdmin()) {
                if (contMasterSudo != null) {
                    if (!UserUtils.getList(contMasterSudo, "email").contains(currentUser.getEmail())) {
                        Log.c("Cu Sudo", cont, currentUser.getEmail());
                        throw new UnauthorizedAccess("User does not match Account");
                    }
                } else if (!UserUtils.getList(cont, "email").contains(currentUser.getEmail())) {
                    Log.c("Fara Sudo", cont, currentUser.getEmail());
                    throw new UnauthorizedAccess("User does not match Account");
                }
            }
            return cont;
        }
        return null;
    }

    public Entity getSuperAdminCont(HttpServletRequest req) throws IllegalStateException, IllegalArgumentException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        // Get mailuri clienti setate in aplicatie ca "superAdministrator" ===BEGIN===
        if (user != null && userService.isUserAdmin()) {
            Query query = new Query("Client");
            query.setFilter(Query.FilterOperator.EQUAL.of("email", user.getEmail()));
            query.setKeysOnly();
            PreparedQuery pQuery = datastore.prepare(query);
            List<Entity> clients = pQuery.asList(FetchOptions.Builder.withLimit(1));
            // Get mailuri clienti setate in aplicatie ca "superAdministrator" ===END===
            if (clients.size() == 0) {
                Entity superAdmin = new Entity("Cont", AbstractUserType.SuperAdministrator);
                superAdmin.setProperty("email", user.getEmail());
                superAdmin.setProperty("tipCont", AbstractUserType.SuperAdministrator);
                superAdmin.setProperty("numeActual", StringUtils.formatEmail(user.getEmail()));
                superAdmin.setProperty("prenumeActual", "");

                return superAdmin;
            }
        }


        if (user == null) {
            if (Utils.isTask(req)) {
                Entity task = new Entity("Cont", AbstractUserType.SuperAdministrator);
                task.setProperty("email", req.getParameter("_emailContCurent"));
                task.setProperty("tipCont", AbstractUserType.SuperAdministrator);
                task.setProperty("numeActual", StringUtils.formatEmail(req.getParameter("_emailContCurent")));
                task.setProperty("prenumeActual", "");
                return task;
            } else {
                return null;
            }
        }
        return null;
    }

    private void sendRedirect(String reqUrl) throws SendRedirect {
        if (req.getQueryString() != null) {
            throw new SendRedirect(reqUrl.replace("http:", "https:") + "?" + req.getQueryString());
        } else {
            throw new SendRedirect(reqUrl.replace("http:", "https:"));
        }
    }

    private Boolean uniqueInstanceCookie() {
        if (!Utils.isTask(req) && isRequiredUniqueAuthCookie() && contCurent != null && currentUser != null) {
            Text currentCookieValueText = (Text) contCurent.getProperty("cookieValue");
            String currentCookieValue = null;
            if (currentCookieValueText != null) {
                currentCookieValue = currentCookieValueText.getValue();
            }
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (c.getName().equals("SACSID") || c.getName().equals("ACSID")) {
                        String cookieValue = c.getValue();
                        if (currentCookieValue != null && currentCookieValue.equals(cookieValue)) {
                            return true;
                        }
                        if (currentCookieValue != null) {
                            Integer hashCode = cookieValue.hashCode();
                            Integer currentHashCode = currentCookieValue.hashCode();
                            NamespaceManager.set("");
                            Query q = new Query("InactiveCookie");
                            q.setFilter(Query.FilterOperator.EQUAL.of("hashCode", hashCode));
                            PreparedQuery pq = datastore.prepare(q);
                            List<Entity> inactiveCookies = pq.asList(FetchOptions.Builder.withDefaults());
                            if (inactiveCookies != null && !inactiveCookies.isEmpty()) {
                                for (Entity inactiveCookie : inactiveCookies) {
                                    String inactiveCookieValue = ((Text) inactiveCookie.getProperty("cookieValue")).getValue();
                                    if (inactiveCookieValue.equals(cookieValue)) {
                                        return false;
                                    }
                                }
                            }
                            Entity newInactiveCookie = new Entity("InactiveCookie");
                            newInactiveCookie.setUnindexedProperty("hashCont", contCurent.getKey());
                            newInactiveCookie.setProperty("hashCode", currentHashCode);
                            newInactiveCookie.setUnindexedProperty("cookieValue", new Text(currentCookieValue));
                            datastore.put(newInactiveCookie);
                        }

                        try {
                            Entity dbCont = datastore.get(contCurent.getKey());
                            dbCont.setProperty("cookieValue", new Text(cookieValue));
                            contCurent.setProperty("cookieValue", new Text(cookieValue));
                            datastore.put(dbCont);
                        } catch (EntityNotFoundException ignored) {
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isRequiredUniqueAuthCookie() {
        return false;
    }

    public void logAuthInfo() {
        String email = getCurrentEmail();
        if (email == null){
            email = "null";
        }
        if (contCurent != null) {
            if (userService.getCurrentUser() != null) {
                Log.c("Utilizator logat autentificat: " + email);
            } else if (Utils.isTask(req)) {
                Log.c("Utilizator nelogat autentificat prin task:" + email);
            } else {
                Log.d("Utilizator nelogat autentificat: " + email);
            }
        } else {
            if (userService.getCurrentUser() != null) {
                Log.c("Utilizator logat neautentificat: " + email);
            }
            Log.c("Utilizator nelogat.");
        }
        // Utils.echo(Level.CONFIG, "Memory free: " + ((Runtime.getRuntime().freeMemory()) / 1024 / 1024));
        // Utils.echo(Level.CONFIG, "Total Memory" + Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }

    @SuppressWarnings("unused")
    private void checkAjax(AbstractIController controller, HttpServletRequest req) throws InvalidField {
        if (!isAjax()) {
            throw new InvalidField(InvalidField.i18n("admin.backend.ajaxException", "name", controller.getClass().getSimpleName()));
        }
    }

    private boolean isAjax() {
        return req.getHeader("X-Requested-With") != null && req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest");
    }

    private void checkRole(AbstractIController controller, Entity cont) throws UnauthorizedAccess, InvalidField {

        RequiredType reqType = controller.getClass().getAnnotation(RequiredType.class);
        if (reqType == null) {
            return; // allow every1 to access
        }
        if (cont == null) {
            UserService userService = UserServiceFactory.getUserService();
            User usr = userService.getCurrentUser();
            String email = "";
            if (usr != null) {
                email = usr.getEmail();
            }
            throw new UnauthorizedAccess("[Cont not found] " + email);
        }

        String[] roles = reqType.value();

        boolean found = false;
        for (String ut : roles) {
            if (UserUtils.checkTipCont(cont, ut)) {
                found = true;
            }
        }
        if (!found) {
            throw new InvalidField("admin.backend.accessDenied");
        }
    }

    @SuppressWarnings("unused")
    public boolean isUserAdmin() {
        if (currentUser == null) {
            return false;
        }
        return userService.isUserAdmin();
    }

}
