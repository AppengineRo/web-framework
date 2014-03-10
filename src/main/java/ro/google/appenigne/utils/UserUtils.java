package ro.google.appenigne.utils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import ro.google.appenigne.exceptions.UnauthorizedAccess;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserUtils {

    /**
     * @param req
     * @return
     * @throws com.google.appengine.api.datastore.EntityNotFoundException
     * @throws ro.google.appenigne.exceptions.UnauthorizedAccess
     */
    public static Entity getCont(String _hashContCurent, HttpServletRequest req) throws IllegalStateException, EntityNotFoundException, UnauthorizedAccess {
        UserService userService = UserServiceFactory.getUserService();
        Datastore datastore = new Datastore();
        Key keyCont;
        try {
            keyCont = KeyFactory.stringToKey(_hashContCurent);
        } catch (NullPointerException npe) {
            return null;
        } catch (IllegalArgumentException iae) {
            return null;
        }

        if (keyCont.getName() != null) {
            if (keyCont.getName().equals(UserType.SuperAdministrator.toString())) {
                return getSuperAdminCont(req);
            }
            if (keyCont.getName().equals(UserType.Candidat.toString())) {
                return getCandidatCont(null);
            }
        } else {
            Entity cont = datastore.getFromMemOrDb(_hashContCurent);
            Entity contMasterSudo = datastore.getFromMemOrDb(req.getParameter("_hashContMasterSudo"));
            User usr = userService.getCurrentUser();
            if (usr == null) {
                if (Utils.isTask(req)) {
                    return cont;
                } else {
                    return null;
                }
            }
            if (!userService.isUserAdmin()) {
                if (contMasterSudo != null) {
                    if (!getList(contMasterSudo, "email").contains(usr.getEmail())) {
                        Log.echo("Cu Sudo", cont, usr.getEmail());
                        throw new UnauthorizedAccess("User does not match Account");
                    }
                } else if (!getList(cont, "email").contains(usr.getEmail())) {
                    Log.echo("Fara Sudo", cont, usr.getEmail());
                    throw new UnauthorizedAccess("User does not match Account");
                }
            }
            return cont;
        }
        return null;
    }
    public static Entity getSuperAdminCont(HttpServletRequest req) throws IllegalStateException, IllegalArgumentException {
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
                Entity superAdmin = new Entity("Cont", UserType.SuperAdministrator.toString());
                superAdmin.setProperty("email", user.getEmail());
                superAdmin.setProperty("tipCont", UserType.SuperAdministrator.toString());
                superAdmin.setProperty("numeActual", StringUtils.formatEmail(user.getEmail()));
                superAdmin.setProperty("prenumeActual", "");

                return superAdmin;
            }
        }


        if (user == null) {
            if (Utils.isTask(req)) {
                Entity task = new Entity("Cont", UserType.SuperAdministrator.toString());
                task.setProperty("email", req.getParameter("_emailContCurent"));
                task.setProperty("tipCont", UserType.SuperAdministrator.toString());
                task.setProperty("numeActual", StringUtils.formatEmail(req.getParameter("_emailContCurent")));
                task.setProperty("prenumeActual", "");
                return task;
            } else {
                return null;
            }
        }
        return null;
    }

    public static Entity getCandidatCont(Key keyClient) {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user == null) {
            return null;
        }

        Entity candidat = new Entity("Cont", UserType.Candidat.toString());
        candidat.setProperty("email", user.getEmail());
        candidat.setProperty("tipCont", UserType.Candidat.toString());
        candidat.setProperty("numeActual", StringUtils.formatEmail(user.getEmail()));
        candidat.setProperty("prenumeActual", "");
        if (keyClient != null) {
            List<Key> listKeysClient = new ArrayList<>();
            listKeysClient.add(keyClient);
            candidat.setProperty("keyClient", listKeysClient);
        }

        return candidat;
    }

    /**
     * Returns a list with the strings found<br />
     * Does not return null
     *
     * @param entity
     * @param prop
     * @return
     */
    public static List<String> getList(Entity entity, String prop) {
        List<String> extractedEmails = new ArrayList<>();
        if (entity != null) {
            if (entity.getProperty(prop) instanceof String) {
                if (!((String) entity.getProperty(prop)).isEmpty()) {
                    extractedEmails.add(((String) entity.getProperty(prop)));
                }
            } else if (entity.getProperty(prop) instanceof List<?>) {
                for (Object email : (List<?>) entity.getProperty(prop)) {
                    if (email instanceof String && !((String) email).isEmpty()) {
                        extractedEmails.add(((String) email));
                    }
                }
            } else if (entity.getProperty(prop) instanceof Set<?>) {
                for (Object email : (Set<?>) entity.getProperty(prop)) {
                    if (email instanceof String && !((String) email).isEmpty()) {
                        extractedEmails.add(((String) email));
                    }
                }
            }
        }
        return extractedEmails;
    }

    public static String getStudentDisplayName(Entity detaliiStudent) {
        if ((detaliiStudent.getProperty("numeComplet") != null)
                && !((String) detaliiStudent.getProperty("numeComplet")).isEmpty()) {
            return (String) detaliiStudent.getProperty("numeComplet");
        }
        String result = "";
        Boolean numeNastere = false;
        if ((detaliiStudent.getProperty("numeNastere") != null)
                && !((String) detaliiStudent.getProperty("numeNastere")).isEmpty()) {
            result += ((String) detaliiStudent.getProperty("numeNastere")).trim();
            numeNastere = true;
        } else if ((detaliiStudent.getProperty("numeActual") != null)
                && !((String) detaliiStudent.getProperty("numeActual")).isEmpty()) {
            result += ((String) detaliiStudent.getProperty("numeActual")).trim();
        }

        if ((detaliiStudent.getProperty("initiale") != null)
                && !((String) detaliiStudent.getProperty("initiale")).isEmpty()) {
            result += " " + ((String) detaliiStudent.getProperty("initiale")).trim();
        }

        if ((detaliiStudent.getProperty("prenumeActual") != null)
                && !((String) detaliiStudent.getProperty("prenumeActual")).isEmpty()) {
            result += " " + ((String) detaliiStudent.getProperty("prenumeActual")).trim();
        }

        if (numeNastere
                && (detaliiStudent.getProperty("numeActual") != null)
                && !((String) detaliiStudent.getProperty("numeActual")).isEmpty()
                && !detaliiStudent.getProperty("numeNastere").equals(detaliiStudent.getProperty("numeActual"))) {
            result += " (" + ((String) detaliiStudent.getProperty("numeActual")).trim() + ")";
        }
        if (result.isEmpty()) {
            List<String> email = getList(detaliiStudent, "email");
            result = email.get(0);
        }

        return result;
    }

    private static boolean isContAdmin(Entity ent) {
        if (ent.getKind().equals("Cont")
                && (ent.getKey().getName() != null)
                && ent.getKey().getName().equals(UserType.SuperAdministrator.toString())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkTipCont(Entity cont, UserType tipCont) {
        if (cont == null) return false;
        if (!cont.getKind().equals("Cont")) {
            return false;
        }
        switch (tipCont) {
            case SuperAdministrator:
                return isContAdmin(cont);
            case Admin:
            case Secretar:
            case Profesor:
            case Imprumut:
            case Candidat:
            case Student:
            case Parinte:
            case Task:
                if ((cont.getProperty("tipCont") != null)
                        && (UserType.valueOf((String) cont.getProperty("tipCont")) == tipCont)) {
                    return true;
                }
                break;
            default:
                break;
        }

        return false;
    }
}
