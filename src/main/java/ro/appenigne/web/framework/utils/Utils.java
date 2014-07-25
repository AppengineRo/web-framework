package ro.appenigne.web.framework.utils;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ro.appenigne.web.framework.datastore.Datastore;
import ro.appenigne.web.framework.exception.InvalidField;
import ro.appenigne.web.framework.form.FormValidate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class Utils {


    public static Key getDetaliiKey(String hash) {
        return getDetaliiKey(KeyFactory.stringToKey(hash));
    }

    public static Key getDetaliiKey(Key key) {
        if (key.getParent() == null) {
            key = KeyFactory.createKey(key, "Detalii" + key.getKind(), "0");
        }
        return key;
    }

    public static Key getDetaliiKey(Object o) {
        if (o instanceof Key) {
            return getDetaliiKey((Key) o);
        } else if (o instanceof String) {
            return getDetaliiKey((String) o);
        }
        return null;
    }

    /**
     * Seteaza proprietatea ancestor pe baza parentului.
     * Se creaza o lista de ancestori care e compusa din key-ul parentului + lista din proprietatea ancestor a
     * parentului
     *
     * @param parent
     * @param entity
     * @throws com.google.appengine.api.datastore.EntityNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static void setAncestor(Entity parent, Entity entity) throws EntityNotFoundException {
        ArrayList<Key> ancestor = new ArrayList<>();
        ancestor.add(parent.getKey());
        ancestor.addAll((List<Key>) parent.getProperty("ancestor"));
        entity.setProperty("ancestor", ancestor);
    }

    /**
     * Returneaza din lista "ancestor" a unei entitati key-ul care are kind-ul specificat.
     *
     * @param entity
     * @param kind
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Key getAncestor(Entity entity, String kind) {
        if (entity.getKind().equals(kind)) {
            return entity.getKey();
        }
        if (entity.getKey().getParent() != null && entity.getKey().getParent().getKind().equals(kind)) {
            return entity.getKey().getParent();
        }

        ArrayList<Key> ancestor = (ArrayList<Key>) entity.getProperty("ancestor");
        if ((ancestor != null) && !ancestor.isEmpty()) {
            for (Key keyAncestor : ancestor) {
                if (keyAncestor.getKind().equalsIgnoreCase(kind)) {
                    return keyAncestor;
                }
            }
        }
        return null;
    }

    /**
     * @param entities
     * @param req
     */
    public static void filterDeletedEntities(List<Entity> entities, HttpServletRequest req) {
        if (((req.getParameter("_showTrash") == null) || !req.getParameter("_showTrash").equals("true"))
                && !entities.isEmpty()) {
            filterDeletedEntities(entities.iterator());
        }
    }

    public static void filterDeletedEntities(List<Entity> entities) {
        filterDeletedEntities(entities.iterator());
    }

    public static void filterDeletedEntities(Iterator<Entity> itr) {
        LinkedHashMap<Key, Entity> ancestorsCache = new LinkedHashMap<>();
        filterDeletedEntities(itr, ancestorsCache);
    }

    public static void filterDeletedEntities(Iterator<Entity> itr, Map<Key, Entity> oldCache) {
        LinkedHashMap<Key, Entity> ancestorsCache = new LinkedHashMap<>(oldCache);
        while (itr.hasNext()) {
            Entity entity = itr.next();
            if (entity.getKind().equals("Cont") || entity.getKind().equals("DetaliiCont")) {
                if (entity.getProperty("keyClient") == null) {
                    itr.remove();
                }
            } else {
                if ((entity.getProperty("_sters") != null) || hasTrashedAcestor(entity, ancestorsCache)) {
                    itr.remove();
                }
            }
        }
    }

    public static void filterDeletedEntities(Map<Key, Entity> entities, HttpServletRequest req) {
        if (((req.getParameter("_showTrash") == null) || !req.getParameter("_showTrash").equals("true"))
                && !entities.isEmpty()) {
            Iterator<Entity> itr = entities.values().iterator();
            filterDeletedEntities(itr, entities);
        }
    }

    public static void filterDeletedEntities(Map<Key, Entity> entities) {
        Iterator<Entity> itr = entities.values().iterator();
        filterDeletedEntities(itr, entities);
    }

    public static boolean isTask(HttpServletRequest req) {
        String taskName = req.getHeader("X-AppEngine-TaskName");
        String taskQueue = req.getHeader("X-AppEngine-QueueName");
        return ((taskName != null) && !taskName.isEmpty() && (taskQueue != null) && !taskQueue.isEmpty());
    }

    public static String getCurrentEmail(HttpServletRequest req) {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (Utils.isTask(req)) {
            return req.getParameter("_emailContCurent");
        } else if (user != null) {
            return user.getEmail();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean hasTrashedAcestor(Entity entity, Map<Key, Entity> ancestorsCache) {
        Datastore datastore = new Datastore(null);
        if (entity.getProperty("_sters") != null) {
            return true;
        }
        if ((entity.getProperty("ancestor") != null) && !((ArrayList<Key>) entity.getProperty("ancestor")).isEmpty()) {
            ArrayList<Key> ancestors = (ArrayList<Key>) entity.getProperty("ancestor");
            ArrayList<Key> getAncestors = new ArrayList<>();
            for (Key ancestor : ancestors) {
                if (ancestorsCache.get(ancestor) == null) {
                    getAncestors.add(ancestor);
                }
            }
            if (getAncestors.size() > 0) {
                ancestorsCache.putAll(datastore.get(getAncestors));
            }
            for (Key ancestor : ancestors) {
                if ((ancestorsCache.get(ancestor) == null)
                        || (ancestorsCache.get(ancestor).getProperty("_sters") != null)) {
                    return true;
                }
            }
        }
        /* if (entity.getParent() != null) {
            Key keyParent = entity.getParent();
            ArrayList<Key> getParents = new ArrayList<>();
            while (keyParent != null) {
                if (ancestorsCache.get(keyParent) == null) {
                    getParents.add(keyParent);
                }
                keyParent = keyParent.getParent();
            }
            if (getParents.size() > 0) {
                ancestorsCache.putAll(datastore.get(getParents));
            }
            while (keyParent != null) {
                if ((ancestorsCache.get(keyParent) == null)
                        || (ancestorsCache.get(keyParent).getProperty("_sters") != null)) {
                    return true;
                }
                keyParent = keyParent.getParent();
            }
        }*/
        return false;
    }

    @SuppressWarnings("unchecked")
    public static List<LinkedHashMap<String, String>> extractFields(String json) {
        Gson gson = GsonUtils.getGson();
        LinkedHashMap<String, Object> form = (LinkedHashMap<String, Object>) gson.fromJson(json, Object.class);

        ArrayList<Object> formElements = (ArrayList<Object>) form.get("elements");
        List<LinkedHashMap<String, Object>> elementeJson = FormValidate.getFields(formElements);
        List<LinkedHashMap<String, String>> extractedFields = new ArrayList<>();
        Integer nrCrt = -1;
        for (LinkedHashMap<String, Object> element : elementeJson) {
            nrCrt++;
            if (element.get("type").equals("submit") || element.get("type").equals("button")) {
                continue;
            }
            Integer currentIndex = nrCrt;
            if (element.get("template") != null) {
                if (element.get("class").equals("fix-label-special")) {
                    LinkedHashMap<String, Object> nextElement = elementeJson.get(currentIndex + 1);
                    while ((nextElement != null) && (nextElement.get("template") == null)) {
                        currentIndex++;
                        if ((nextElement.get("class") != null)
                                && nextElement.get("class").equals("fix-input-special-label")) {
                            LinkedHashMap<String, Object> nextInputElement = elementeJson.get(currentIndex);
                            if ((nextInputElement.get("name") != null) && (nextInputElement.get("dbType") != null)) {
                                LinkedHashMap<String, String> field = new LinkedHashMap<>();
                                field.put("html",
                                        element.get("html") + ": " + nextElement.get("html"));
                                field.put("name", (String) nextInputElement.get("name"));
                                field.put("fieldset", (String) nextInputElement.get("fieldset"));
                                field.put("class", (String) nextInputElement.get("class"));
                                field.put("dbType", (String) nextInputElement.get("dbType"));
                                field.put("clone", getStringFromObj(nextInputElement.get("clone")));
                                field.put("data-edit-database", (String) nextInputElement.get("data-edit-database"));
                                field.put("data-edit-config", (String) nextInputElement.get("data-edit-config"));
                                Map<String, Object> options = (Map<String, Object>) nextInputElement.get("options");
                                if (options == null) {
                                    field.put("options", null);
                                } else {
                                    field.put("options", StringUtils.join("|", options.keySet()));
                                }

                                if (nextElement.get("numarZecimale") != null) {
                                    field.put("numarZecimale", nextElement.get("numarZecimale").toString());
                                }
                                if (nextElement.get("aproximareZecimale") != null) {
                                    field.put("aproximareZecimale", nextElement.get("aproximareZecimale").toString());
                                }
                                if ((nextElement.get("formula") != null)
                                        && !((String) nextElement.get("formula")).isEmpty()) {
                                    field.put("formula", (String) nextElement.get("formula"));
                                }

                                extractedFields.add(field);
                            }
                        }
                        try {
                            nextElement = elementeJson.get(currentIndex);
                        } catch (IndexOutOfBoundsException e) {
                            break;
                        }
                    }

                } else if (element.get("class").equals("fix-file-label")) {
                    LinkedHashMap<String, Object> nextElement = elementeJson.get(currentIndex + 1);
                    while (nextElement.get("template") == null) {
                        currentIndex++;
                        if ((nextElement.get("name") != null) && (nextElement.get("dbType") != null)) {
                            LinkedHashMap<String, String> field = new LinkedHashMap<>();
                            field.put("html", (String) element.get("html"));
                            field.put("name", (String) nextElement.get("name"));
                            field.put("fieldset", (String) nextElement.get("fieldset"));
                            field.put("class", (String) nextElement.get("class"));
                            field.put("dbType", (String) nextElement.get("dbType"));
                            field.put("clone", getStringFromObj(nextElement.get("clone")));
                            field.put("data-edit-database", (String) nextElement.get("data-edit-database"));
                            field.put("data-edit-config", (String) nextElement.get("data-edit-config"));
                            Map<String, Object> options = (Map<String, Object>) nextElement.get("options");
                            if (options == null) {
                                field.put("options", null);
                            } else {
                                field.put("options", StringUtils.join("|", options.keySet()));
                            }
                            if (nextElement.get("numarZecimale") != null) {
                                field.put("numarZecimale", nextElement.get("numarZecimale").toString());
                            }
                            if (nextElement.get("aproximareZecimale") != null) {
                                field.put("aproximareZecimale", nextElement.get("aproximareZecimale").toString());
                            }
                            if ((nextElement.get("formula") != null)
                                    && !((String) nextElement.get("formula")).isEmpty()) {
                                field.put("formula", (String) nextElement.get("formula"));
                            }

                            extractedFields.add(field);
                        }
                        try {
                            nextElement = elementeJson.get(currentIndex);
                        } catch (IndexOutOfBoundsException e) {
                            break;
                        }
                    }
                } else {
                    LinkedHashMap<String, Object> nextElement = elementeJson.get(currentIndex + 1);
                    if ((nextElement.get("name") != null) && (nextElement.get("dbType") != null)) {
                        LinkedHashMap<String, String> field = new LinkedHashMap<>();
                        field.put("html", (String) element.get("html"));
                        field.put("name", (String) nextElement.get("name"));
                        field.put("fieldset", (String) nextElement.get("fieldset"));
                        field.put("class", (String) nextElement.get("class"));
                        field.put("dbType", (String) nextElement.get("dbType"));
                        field.put("clone", getStringFromObj(nextElement.get("clone")));
                        field.put("data-edit-database", (String) nextElement.get("data-edit-database"));
                        field.put("data-edit-config", (String) nextElement.get("data-edit-config"));
                        Map<String, Object> options = (Map<String, Object>) nextElement.get("options");
                        if (options == null) {
                            field.put("options", null);
                        } else {
                            field.put("options", StringUtils.join("|", options.keySet()));
                        }
                        if (nextElement.get("numarZecimale") != null) {
                            field.put("numarZecimale", nextElement.get("numarZecimale").toString());
                        }
                        if (nextElement.get("aproximareZecimale") != null) {
                            field.put("aproximareZecimale", nextElement.get("aproximareZecimale").toString());
                        }
                        if ((nextElement.get("formula") != null)
                                && !((String) nextElement.get("formula")).isEmpty()) {
                            field.put("formula", (String) nextElement.get("formula"));
                        }

                        extractedFields.add(field);
                    }
                }
            }
        }
        return extractedFields;
    }

    private static String getStringFromObj(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }

    public static String getCodTara(String tara) {
        Type listType = new TypeToken<TreeMap<String, String>>() {
        }.getType();
        Datastore datastore = new Datastore(null);
        NamespaceManager.set("");
        Key keyTaraStart = KeyFactory.createKey("Config", "tara");
        Key keyTaraEnd = KeyFactory.createKey("Config", "tarb");
        Query q = new Query("Config");
        q.setFilter(CompositeFilterOperator.and(
                FilterOperator.GREATER_THAN_OR_EQUAL.of(Entity.KEY_RESERVED_PROPERTY, keyTaraStart),
                FilterOperator.LESS_THAN.of(Entity.KEY_RESERVED_PROPERTY, keyTaraEnd)
        ));
        PreparedQuery pq = datastore.prepare(q);
        List<Entity> configTari = pq.asList(FetchOptions.Builder.withDefaults());
        if (!configTari.isEmpty()) {
            for (Entity tariEntity : configTari) {
                String tariString = ((Text) tariEntity.getProperty("value")).getValue();
                TreeMap<String, String> tari = GsonUtils.getGson().fromJson(tariString, listType);
                for (Entry<String, String> key : tari.entrySet()) {
                    if (key.getKey().equalsIgnoreCase(tara)) {
                        return key.getValue();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Returneaza true daca serverul ruleaza local
     *
     * @return
     */
    public static boolean isLocalServer() {
        return (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development);
    }

    public static boolean isAppVersion(String version) {
        return SystemProperty.applicationVersion.get().contains(version);
    }


    @SuppressWarnings("unchecked")
    public static void importFeedback(BlobKey blobKey, String mesaj, HttpServletRequest req) {
        String importType = req.getParameter("importType");
        Object rowNr = req.getParameter("_rowNr");
        try {
            Key keyEntity = KeyFactory.stringToKey((String) rowNr);
            if (!keyEntity.getKind().contains("Detalii")) {
                keyEntity = KeyFactory.createKey(keyEntity, "Detalii" + keyEntity.getKind(), "0");
            }
            Datastore datastore = new Datastore(req);
            Entity entity = datastore.get(keyEntity);
            ArrayList<String> feedBacks = new ArrayList<>();
            if (entity.hasProperty("_feedback")) {
                feedBacks = (ArrayList<String>) entity.getProperty("_feedback");
            }
            if (!feedBacks.contains(mesaj)) {
                feedBacks.add(mesaj);
            } else {
                feedBacks.remove(mesaj);
                feedBacks.add(mesaj);
            }
            entity.setProperty("_feedback", feedBacks);

            if (entity.getKind().equals("DetaliiStudent")) {
                entity.setProperty("_newFeedback", true);
            }
            datastore.put(entity);
        } catch (IllegalArgumentException | EntityNotFoundException | NullPointerException f) {
            Utils.saveFeedBack(blobKey, importType, rowNr, mesaj, req);
        }
    }

    public static void saveFeedBack(BlobKey blobKey, String importType, Object rowNr, String mesaj, HttpServletRequest req) {
        Datastore datastore = new Datastore(req);
        NamespaceManager.set("");
        Entity importEntity = new Entity("ImportFeedback");
        importEntity.setProperty("blobKey", blobKey);
        importEntity.setProperty("importType", importType);
        importEntity.setProperty("rowNr", rowNr);
        if (mesaj.length() > 400) {
            importEntity.setProperty("message", mesaj.substring(0, 400));
        } else {
            importEntity.setProperty("message", mesaj);
        }
        datastore.put(importEntity);
    }

    public static String getAppId() {
        String result = ApiProxy.getCurrentEnvironment().getAppId();
        return result.substring(result.indexOf("~") + 1);
    }

}
