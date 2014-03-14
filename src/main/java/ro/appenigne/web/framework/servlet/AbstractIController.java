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
import ro.appenigne.web.framework.annotation.RequiredLogIn;
import ro.appenigne.web.framework.annotation.RequiredType;
import ro.appenigne.web.framework.annotation.XssCheck;
import ro.appenigne.web.framework.datastore.Datastore;
import ro.appenigne.web.framework.datastore.KeysOnlyDatastoreCallback;
import ro.appenigne.web.framework.exception.InvalidAuthCookie;
import ro.appenigne.web.framework.exception.InvalidField;
import ro.appenigne.web.framework.exception.UnauthorizedAccess;
import ro.appenigne.web.framework.request.FilteredRequest;
import ro.appenigne.web.framework.request.TrimRequest;
import ro.appenigne.web.framework.utils.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public User currentUser;
    public UserService userService;

    public void run(HttpServletRequest req, HttpServletResponse resp, Entity contCurent) {
        init(req, resp, contCurent);
        this.userService = UserServiceFactory.getUserService();
        this.currentUser = userService.getCurrentUser();
        createDatastore();
        createDatastoreCallbacks();
        preExecute();
        try {
            finish();
        } catch (ExecutionException | InterruptedException e) {
            Log.s(e);
        }
    }

    private void init(HttpServletRequest req, HttpServletResponse resp, Entity contCurent) {
        this.req = req;
        this.resp = resp;
        if (contCurent != null) {
            this.contCurent = contCurent;
        }
    }

    /**
     * Default implementation adds a datastore callback that prevents puts on empty entities
     */
    public void createDatastoreCallbacks() {
        datastore.addPrePut(new KeysOnlyDatastoreCallback());
    }

    public void createDatastore() {
        this.datastore = new Datastore();
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

    private void finish() throws ExecutionException, InterruptedException {
        datastore.commit();
    }

    public void preExecute() {
        //intra "OPTIONS /do/ajax/ HTTP/1.1" 200 0 - "Microsoft Office Protocol Discovery"
        //if ("OPTIONS".equals(req.getMethod())) return;
        String reqUrl = req.getRequestURL().toString();
        String appDomain = Utils.getAppId() + ".appspot.com";

        if (!Utils.isTask(req) && (reqUrl.startsWith("http://" + appDomain) || reqUrl.startsWith("https://" + appDomain))) {
            if (req.getHeader("X-Requested-With") == null || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
                String domeniu = System.getProperty("domeniu");
                if (domeniu != null && !domeniu.isEmpty()) {
                    reqUrl = reqUrl.replace(appDomain, domeniu);
                    if (sendRedirect(reqUrl)) return;
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
            if (sendRedirect(reqUrl)) return;
        }


        HttpServletRequest filteredReq = new FilteredRequest(req);
        HttpServletRequest trimReq = new TrimRequest(req);

        String queryLink = (trimReq.getQueryString() != null) ? "/?" + trimReq.getQueryString() : "/";
        try {
            boolean searchController = false;
            Logger log = Logger.getLogger("");
            try {
                contCurent = getCont(trimReq.getParameter(_hashContCurentParamName));
                AbstractIController controller = this;
                if (controller.getClass().getSimpleName().equalsIgnoreCase("search")) {
                    searchController = true;
                }
                RequiredLogIn reqLogIn = controller.getClass().getAnnotation(RequiredLogIn.class);
                XssCheck xssCheck = controller.getClass().getAnnotation(XssCheck.class);
                if (reqLogIn != null && reqLogIn.value()) {
                    this.checkRole(controller, contCurent);
                }
                // we don't need to check the backend version since we are using modules now (and dispatch.xml )
                // and we have no more problems with double authentication
                if (/*!Utils.isAppVersion("backend") && */!uniqueInstanceCookie()) {
                    throw new InvalidAuthCookie("uniqueInstanceCookie");
                }
                this.logAuthUser();
                if (controller.getClass().getSimpleName().equals("SalveazaTemplate")) {
                    controller.init(req, resp, contCurent);
                    controller.execute();
                } else {
                    if (xssCheck != null && !xssCheck.value()) {
                        controller.init(trimReq, resp, contCurent);
                        controller.execute();
                    } else {
                        controller.init(filteredReq, resp, contCurent);
                        controller.execute();
                    }
                }
                resp.setHeader("appV", SystemProperty.applicationVersion.get());
            } catch (UnauthorizedAccess e) {
                log.log(Level.CONFIG, e.getClass().getSimpleName(), e);
                if (req.getHeader("X-Requested-With") == null
                        || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
                    resp.sendRedirect(userService.createLogoutURL(queryLink));
                } else {
                    resp.reset();
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().print(e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (InvalidField e) {
                if (Utils.isTask(req)) {
                    BlobKey blobKey = null;
                    if (req.getParameter("_blobKey") != null) {
                        blobKey = new BlobKey(req.getParameter("_blobKey"));
                    }
                    Utils.importFeedback(blobKey,
                            req.getParameter("importType"),
                            req.getParameter("_rowNr"),
                            e.getMessage());
                } else {
                    //resp.reset();
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().print(e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                log.log(Level.CONFIG, e.getClass().getSimpleName(), e);

            } /*catch (Notification e) {
                resp.setContentType("text/plain; charset=UTF-8");
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                Queue queue = QueueFactory.getQueue("bulk");
                TaskOptions taskOptions = TaskOptions.Builder.withUrl("/do/bug/SalveazaBug");
                taskOptions.param("_hashContCurent", KeyFactory.keyToString(cont.getKey()));
                taskOptions.param("_emailContCurent", Utils.getCurrentEmail(req));
                taskOptions.param("subject", "JAVA Notification ERROR");
                taskOptions.param("message", e.getMessage());
                taskOptions.param("jsError", "true");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                taskOptions.param("html", sw.toString().replaceAll("\n", "<br />"));
                queue.add(taskOptions);
            } catch (SilentNotification e) {
                Queue queue = QueueFactory.getQueue("bulk");
                TaskOptions taskOptions = TaskOptions.Builder.withUrl("/do/bug/SalveazaBug");
                taskOptions.param("_hashContCurent", KeyFactory.keyToString(cont.getKey()));
                taskOptions.param("_emailContCurent", Utils.getCurrentEmail(req));
                taskOptions.param("subject", "JAVA Notification ERROR");
                taskOptions.param("message", e.getMessage());
                taskOptions.param("jsError", "true");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                taskOptions.param("html", sw.toString().replaceAll("\n", "<br />"));
                queue.add(taskOptions);
            }*/ catch (EntityNotFoundException | ApiProxy.CapabilityDisabledException e) {
                resp.setContentType("text/plain; charset=UTF-8");
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IllegalStateException e) {
                resp.setContentType("text/plain; charset=UTF-8");
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                this.preExecute();
            } catch (ApiProxy.OverQuotaException e) {
                log.log(Level.CONFIG, e.getClass().getSimpleName(), e);
            } catch (DeadlineExceededException e) {
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print("admin.backend.deadlineExceededException");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (OutOfMemoryError e) {
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                if (searchController) {
                    resp.getWriter().print("admin.backend.outOfSearchMemoryError");
                } else {
                    resp.getWriter().print("admin.backend.outOfMemoryError");
                }
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            } catch (DatastoreFailureException e) {
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            Logger log = Logger.getLogger("OOPS");
            log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
        }
    }

    public Entity getCont(String _hashContCurent) throws EntityNotFoundException, UnauthorizedAccess {
        Key keyCont;
        try {
            keyCont = KeyFactory.stringToKey(_hashContCurent);
        } catch (NullPointerException npe) {
            Log.d(npe);
            return null;
        } catch (IllegalArgumentException iae) {
            Log.d(iae);
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
                        Log.echo("Cu Sudo", cont, currentUser.getEmail());
                        throw new UnauthorizedAccess("User does not match Account");
                    }
                } else if (!UserUtils.getList(cont, "email").contains(currentUser.getEmail())) {
                    Log.echo("Fara Sudo", cont, currentUser.getEmail());
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
            Datastore datastore = new Datastore();
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

    private boolean sendRedirect(String reqUrl) {
        try {
            if (req.getQueryString() != null) {
                resp.sendRedirect(reqUrl.replace("http:", "https:") + "?" + req.getQueryString());
            } else {
                resp.sendRedirect(reqUrl.replace("http:", "https:"));
            }
            return true;
        } catch (IOException e) {
            Logger log = Logger.getLogger(this.getClass().getSimpleName());
            log.log(Level.SEVERE, "", e);
        }
        return false;
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
                        Datastore datastore = new Datastore();
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

    private void logAuthUser() {
        if (contCurent != null) {
            if (userService.getCurrentUser() != null) {
                Log.echo(Level.CONFIG, "Utilizator logat autentificat: " + getCurrentEmail());
            } else if (Utils.isTask(req)) {
                Log.echo(Level.CONFIG, "Utilizator nelogat autentificat prin task:" + getCurrentEmail());
            } else {
                Log.echo(Level.SEVERE, "Utilizator nelogat autentificat: " + getCurrentEmail());
            }
        } else {
            if (userService.getCurrentUser() != null) {
                Log.echo(Level.CONFIG, "Utilizator logat neautentificat: " + getCurrentEmail());
            }
            Log.echo(Level.CONFIG, "Utilizator nelogat.");
        }
        // Utils.echo(Level.CONFIG, "Memory free: " + ((Runtime.getRuntime().freeMemory()) / 1024 / 1024));
        // Utils.echo(Level.CONFIG, "Total Memory" + Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }

    @SuppressWarnings("unused")
    private void checkAjax(AbstractIController controller, HttpServletRequest req) throws InvalidField {
        if (req.getHeader("X-Requested-With") == null
                || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
            throw new InvalidField(InvalidField.i18n("admin.backend.ajaxException", "name", controller.getClass().getSimpleName()));
        }

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

    public boolean isUserAdmin() {
        if (currentUser == null) {
            return false;
        }
        return userService.isUserAdmin();
    }


}
