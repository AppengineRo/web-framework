package ro.appenigne.web.framework.utils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import ro.appenigne.web.framework.datastore.Datastore;
import ro.appenigne.web.framework.exception.UnauthorizedAccess;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserUtils {

    /**
     * Returns a list with the strings found<br />
     * Does not return null
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
                && ent.getKey().getName().equals(AbstractUserType.SuperAdministrator)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkTipCont(Entity cont, String tipCont) {
        if (cont == null) return false;
        if (!cont.getKind().equals("Cont")) {
            return false;
        }
        switch (tipCont) {
            case AbstractUserType.SuperAdministrator:
                return isContAdmin(cont);
            default:
                if (cont.getProperty("tipCont") != null
                        && cont.getProperty("tipCont").equals(tipCont)) {
                    return true;
                }
                break;
        }

        return false;
    }
}
