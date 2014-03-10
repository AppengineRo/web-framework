package ro.google.appenigne;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy.CapabilityDisabledException;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import com.google.apphosting.api.DeadlineExceededException;
import ro.google.appenigne.annotation.RequiredLogIn;
import ro.google.appenigne.annotation.RequiredType;
import ro.google.appenigne.annotation.XssCheck;
import ro.google.appenigne.exceptions.InvalidField;
import ro.google.appenigne.exceptions.RepeatTaskException;
import ro.google.appenigne.exceptions.UnauthorizedAccess;
import ro.google.appenigne.request.FilteredRequest;
import ro.google.appenigne.request.TrimRequest;
import ro.google.appenigne.utils.*;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class RouterServlet extends HttpServlet {

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) {
        //intra "OPTIONS /do/ajax/ HTTP/1.1" 200 0 - "Microsoft Office Protocol Discovery"
        //if ("OPTIONS".equals(req.getMethod())) return;

        String reqUrl = req.getRequestURL().toString();
        String appDomain = Utils.getAppId() + ".appspot.com";

        if (!Utils.isTask(req) && (reqUrl.startsWith("http://" + appDomain) || reqUrl.startsWith("https://" + appDomain))) {
            if (req.getHeader("X-Requested-With") == null || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
                String domeniu = System.getProperty("domeniu");
                if (domeniu != null && !domeniu.isEmpty()) {
                    reqUrl = reqUrl.replace(appDomain, domeniu);
                    if (sendRedirect(req, resp, reqUrl)) return;
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
            if (sendRedirect(req, resp, reqUrl)) return;
        }


        HttpServletRequest filteredReq = new FilteredRequest(req);
        HttpServletRequest trimReq = new TrimRequest(req);
        UserService userService = UserServiceFactory.getUserService();
        String queryLink = (trimReq.getQueryString() != null) ? "/?" + trimReq.getQueryString() : "/";
        Entity cont;
        try {
            boolean searchController = false;
            Logger log = Logger.getLogger("");
            try {
                cont = UserUtils.getCont(trimReq.getParameter("_hashContCurent"), trimReq);
                IController controller = this.getController(trimReq);
                if (controller.getClass().getSimpleName().equalsIgnoreCase("search")) {
                    searchController = true;
                }
                RequiredLogIn reqLogIn = controller.getClass().getAnnotation(RequiredLogIn.class);
                XssCheck xssCheck = controller.getClass().getAnnotation(XssCheck.class);
                if (reqLogIn != null && reqLogIn.value()) {
                    this.checkRole(controller, cont);
                }
                // we don't need to check the backend version since we are using modules now (and dispatch.xml )
                // and we have no more problems with double authentication
                if (/*!Utils.isAppVersion("backend") && */!uniqueInstanceCookie(req, cont, userService)) {
                    throw new UnauthorizedAccess("uniqueInstanceCookie");
                }
                this.logMemory(cont, userService, req);
                if (controller.getClass().getSimpleName().equals("SalveazaTemplate")) {
                    controller.run(req, resp, cont);
                } else {
                    if (xssCheck != null && !xssCheck.value()) {
                        controller.run(trimReq, resp, cont);
                    } else {
                        controller.run(filteredReq, resp, cont);
                    }
                }
                resp.addHeader("appV", SystemProperty.applicationVersion.get());
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
            }*/ catch (EntityNotFoundException | ServletException | RepeatTaskException | InterruptedException | CapabilityDisabledException e) {
                resp.setContentType("text/plain; charset=UTF-8");
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                resp.reset();
                resp.getWriter().print(e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IllegalStateException e) {
                resp.setContentType("text/plain; charset=UTF-8");
                log.log(Level.SEVERE, e.getClass().getSimpleName(), e);
                this.service(req, resp);
            } catch (OverQuotaException e) {
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

    private boolean sendRedirect(HttpServletRequest req, HttpServletResponse resp, String reqUrl) {
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

    private Boolean uniqueInstanceCookie(HttpServletRequest req, Entity contCurent, UserService userService) {
        List<String> emailsContCurent = UserUtils.getList(contCurent, "email");

        if (contCurent != null && userService.getCurrentUser() != null) {
            if (!UserUtils.checkTipCont(contCurent, UserType.Secretar) && !UserUtils.checkTipCont(contCurent, UserType.Admin)) {
                return true;
            }
            if (emailsContCurent.contains(userService.getCurrentUser().getEmail()) &&
                    !UserUtils.checkTipCont(contCurent, UserType.SuperAdministrator) &&
                    !UserUtils.checkTipCont(contCurent, UserType.Candidat)) {
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
                                q.setFilter(FilterOperator.EQUAL.of("hashCode", hashCode));
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
        }
        return true;
    }

    private void logMemory(Entity cont, UserService userService, HttpServletRequest req) {
        if (cont != null) {
            if (userService.getCurrentUser() != null) {
                Log.echo(Level.CONFIG, "Utilizator logat autentificat: " + userService.getCurrentUser().getEmail());
            } else if (Utils.isTask(req)) {
                Log.echo(Level.CONFIG, "Utilizator nelogat autentificat prin task:" + Utils.getCurrentEmail(req));
            } else {
                Log.echo(Level.SEVERE, "Utilizator nelogat autentificat: " + Utils.getCurrentEmail(req));
            }
        } else {
            if (userService.getCurrentUser() != null) {
                Log.echo(Level.CONFIG, "Utilizator logat neautentificat: " + userService.getCurrentUser().getEmail());
            }
            Log.echo(Level.CONFIG, "Utilizator nelogat.");
        }
        // Utils.echo(Level.CONFIG, "Memory free: " + ((Runtime.getRuntime().freeMemory()) / 1024 / 1024));
        // Utils.echo(Level.CONFIG, "Total Memory" + Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }

    @SuppressWarnings("unused")
    private void checkAjax(IController controller, HttpServletRequest req) throws InvalidField {
        if (req.getHeader("X-Requested-With") == null
                || !req.getHeader("X-Requested-With").equalsIgnoreCase("XMLHttpRequest")) {
            throw new InvalidField(InvalidField.i18n("admin.backend.ajaxException", "name", controller.getClass().getSimpleName()));
        }

    }

    /**
     * Returns the IController instance for the request
     *
     * @param req
     * @return
     * @throws UnauthorizedAccess
     */
    private IController getController(HttpServletRequest req) throws UnauthorizedAccess {

        String controllerClassName = req.getPathInfo();
        if (controllerClassName == null) {
            controllerClassName = "";
        }
        if (!controllerClassName.isEmpty() && controllerClassName.charAt(0) == '/') {
            controllerClassName = controllerClassName.substring(1);
        }
        controllerClassName = controllerClassName.replace('/', '.');

        IController controller = null;
        if (!controllerClassName.isEmpty()) {
            controller = this.getController(controllerClassName);
            // if the controller with that class name wasn't found...
            if (controller == null) {
                throw new UnauthorizedAccess(InvalidField.i18n("admin.backend.controllerNotFound", "name", controllerClassName)); // default to the error controller
            }
        } else {
            //controller = new ro.adma.index();
        }
        return controller;
    }

    /**
     * Returns the string-specified controller instance
     *
     * @param className
     * @return
     */
    private IController getController(String className) {
        Class<?> controllerClass;
        try {
            Log.echo(className);
            controllerClass = Class.forName(this.getClass().getPackage().getName() + ".controller." + className);
            if (!IController.class.isAssignableFrom(controllerClass)) {
                return null;
            }
            return (IController) controllerClass.newInstance();

        } catch (ClassNotFoundException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private void checkRole(IController controller, Entity cont) throws UnauthorizedAccess, InvalidField {

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

        UserType[] roles = reqType.value();

        boolean found = false;
        for (UserType ut : roles) {
            if (UserUtils.checkTipCont(cont, ut)) {
                found = true;
            }
        }
        if (!found) {
            throw new InvalidField("admin.backend.accessDenied");
        }
    }

    @SuppressWarnings("unused")
    private static void filterDOS(HttpServletRequest req) throws UnauthorizedAccess {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
        UserService userService = UserServiceFactory.getUserService();
        if (userService.getCurrentUser() != null) {
            // is an user logged in
            userService.getCurrentUser().getEmail();
            MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
            syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.SEVERE));
            long requests = syncCache.increment(userService.getCurrentUser().getEmail() + sdf.format(new Date()),
                    1l,
                    0l);
            // System.out.println(userService.getCurrentUser().getEmail()+sdf.format(new Date())+" --> "+requests);
            if (requests > 1) {
                // System.out.println("DOS"+requests);
                // throw new UnauthorizedAccess("");
            }
        } else {
            // make the same with an ip adress;
        }
    }
}
