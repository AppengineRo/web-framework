package ro.google.appenigne.utils;

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
import ro.google.appenigne.exceptions.InvalidField;
import ro.google.appenigne.form.FormValidate;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class Utils {

    public static String getJsFiles(String path, ServletContext context) {
        Object[] files = context.getResourcePaths(path).toArray();
        StringBuilder jsFiles = new StringBuilder();
        Arrays.sort(files);
        for (Object file : files) {
            if (!((String) file).contains(".svn")) {
                jsFiles.append("<script type='text/javascript' src='").append((String) file).append("' ></script>\n");
            }
        }

        return jsFiles.toString();
    }

    public static String getCssFiles(String path, ServletContext context) {
        Object[] files = context.getResourcePaths(path).toArray();
        StringBuilder jsFiles = new StringBuilder();
        Arrays.sort(files);
        for (Object file : files) {
            if (!((String) file).contains(".svn")) {
                jsFiles.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"")
                        .append((String) file)
                        .append("\" />\n");
            }
        }

        return jsFiles.toString();
    }

    public static void removeDuplicates(List<Entity> list) {
        int size = list.size();
        int out = 0;
        {
            final Set<Entity> encountered = new HashSet<>();
            for (int in = 0; in < size; in++) {
                final Entity t = list.get(in);
                final boolean first = encountered.add(t);
                if (first) {
                    list.set(out++, t);
                }
            }
        }
        while (out < size) {
            list.remove(--size);
        }
    }

    public static String getEntityDisplayName(String hash, Datastore datastore) {
        return getEntityDisplayName(KeyFactory.stringToKey(hash), datastore);
    }

    public static String getEntityDisplayName(Key key, Datastore datastore) {
        ArrayList<String> result = new ArrayList<>();
        try {
            Entity entity = datastore.cacheGet(key);
            List<Key> ancestorsKeys = (List<Key>) entity.getProperty("ancestor");
            Map<Key, Entity> ancestors = datastore.cacheGet(ancestorsKeys);
            for (Entity a : ancestors.values()) {
                if ("AnUniversitar".equals(a.getKind())) {
                    result.add((String) a.getProperty("anUniversitar"));
                }
            }
            for (Entity a : ancestors.values()) {
                if ("Semestru".equals(a.getKind())) {
                    result.add((String) a.getProperty("semestru"));
                }
            }
            for (Entity a : ancestors.values()) {
                if ("AnStudiu".equals(a.getKind())) {
                    result.add((String) a.getProperty("anStudiu"));
                }
            }
            for (Entity a : ancestors.values()) {
                if ("Specializare".equals(a.getKind())) {
                    result.add((String) a.getProperty("specializare"));
                }
            }

            switch (entity.getKind()) {
                case "Specializare":
                    result.add((String) entity.getProperty("specializare"));
                    break;
                case "Disciplina":
                    result.add((String) entity.getProperty("disciplina"));
                    break;
                case "Student":
                    result.add((String) entity.getProperty("numeComplet"));
                    break;
            }
        } catch (EntityNotFoundException ignored) {
        }

        return StringUtils.join(" - ", result);
    }

    public static List<String> getEntityDisplayName(Iterable<String> hashes, Datastore datastore) {
        List<String> results = new ArrayList<>();
        for (String hash : hashes) {
            results.add(getEntityDisplayName(KeyFactory.stringToKey(hash), datastore));
        }
        return results;
    }

    public static void setAncestor(Key keyAncestor, Entity entity) throws EntityNotFoundException {
        Datastore datastore = new Datastore();
        Entity parent = datastore.get(keyAncestor);
        setAncestor(parent, entity);
    }

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

    @SuppressWarnings("unchecked")
    public static Entity getActiveStudent(Entity contStudent, Key keyClient) {
        final Datastore datastore = new Datastore();
        if (contStudent != null && keyClient != null) {

            Key keyPersoana = (Key) contStudent.getProperty("keyPersoana");
            List<Long> namespaceStudent = (List<Long>) contStudent.getProperty("namespaceStudent");
            if (namespaceStudent != null && !namespaceStudent.isEmpty()) {
                List<Key> keyIdentitatiStudenti = new ArrayList<>();
                for (Long idNamespace : namespaceStudent) {
                    NamespaceManager.set(Long.toString(idNamespace));
                    Key keyStudent = KeyFactory.createKey("Student", keyPersoana.getName());
                    NamespaceManager.set("");
                    keyIdentitatiStudenti.add(keyStudent);
                }
                Map<Key, Entity> identitatiStudent = datastore.get(keyIdentitatiStudenti);
                filterDeletedEntities(identitatiStudent);
                LinkedHashMap<Key, Entity> specializareStudent = new LinkedHashMap<>();
                List<Key> keySpecializari = new ArrayList<>();
                for (Entity identitate : identitatiStudent.values()) {
                    Key keySpecializare = KeyFactory.createKey("Specializare", Long.parseLong(identitate.getKey().getNamespace()));
                    specializareStudent.put(keySpecializare, identitate);
                    keySpecializari.add(keySpecializare);
                }
                Map<Key, Entity> specializari = datastore.get(keySpecializari);

                filterDeletedEntities(specializari);
                List<Entity> listSpecializari = new ArrayList<>();
                for (Entity spec : specializari.values()) {
                    if (keyClient.equals(Utils.getAncestor(spec, "Client"))) {
                        listSpecializari.add(spec);
                    }
                }

                for (Entity specializare : listSpecializari) {
                    try {
                        specializare.setProperty("dataInceputSemestru", datastore.get(getAncestor(specializare, "Semestru")).getProperty("dataInceput"));
                    } catch (EntityNotFoundException ignored) {
                    }
                }

                Collections.sort(listSpecializari, new Comparator<Entity>() {
                    @Override
                    public int compare(Entity s2, Entity s1) {
                        Date dataSemestru1 = (Date) s1.getProperty("dataInceputSemestru");
                        Date dataSemestru2 = (Date) s2.getProperty("dataInceputSemestru");
                        if (dataSemestru1 != null) {
                            if (dataSemestru2 != null) {
                                int compare = dataSemestru1.compareTo(dataSemestru2);
                                if (compare != 0) {
                                    return compare;
                                } else {
                                    try {
                                        String anStudiu1 = (String) datastore.get(getAncestor(s1, "AnStudiu")).getProperty("anStudiu");
                                        String anStudiu2 = (String) datastore.get(getAncestor(s2, "AnStudiu")).getProperty("anStudiu");
                                        return anStudiu1.compareTo(anStudiu2);
                                    } catch (EntityNotFoundException ignored) {
                                    }
                                }
                            } else {
                                return 1;
                            }
                        } else {
                            if (dataSemestru2 != null) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                        return 0;
                    }
                });
                Key keySpecializareActiva = null;
                for (Entity specializare : listSpecializari) {
                    if (specializare.getProperty("dataInceputSemestru") != null) {
                        if (((Date) specializare.getProperty("dataInceputSemestru")).before(new Date())) {
                            keySpecializareActiva = specializare.getKey();
                            break;
                        }
                    } else {
                        keySpecializareActiva = specializare.getKey();
                        break;
                    }
                }
                if (keySpecializareActiva != null) {
                    return specializareStudent.get(keySpecializareActiva);
                }
            }
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
        Datastore datastore = new Datastore();
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

    /**
     * Returneaza lista de entitati Sterse/Nesterse [request.getParameter("_showTrash")],
     * dupa tip, unde property = value <br />
     * <p/>
     * Parametrul value trebuie sa aiba unul din
     * <a href='https://developers.google.com/appengine/docs/java/datastore/entities#Properties_and_Value_Types' >
     * tipurile acceptate de Entity
     * </a><br />
     * <p/>
     * Parametrul value mai poate fi si un ArrayList de valori de unul din tipurile de mai sus. <br />
     * In acest caz functia va returna reuniunea tuturor rezultatelor pentru fiecare valoare in parte.<br />
     *
     * @param kind     - String
     * @param property - String
     * @param value    -
     * @param request  - HttpServletRequest
     */
    public static List<Entity> simpleQuery(String kind, String property, Object value, HttpServletRequest request) {
        Datastore datastore = new Datastore();
        List<Entity> results = new ArrayList<>();
        if (property == null) {
            Query query = new Query(kind);
            PreparedQuery pQuery = datastore.prepare(query);
            results.addAll(pQuery.asList(FetchOptions.Builder.withDefaults()));
        } else if (value == null) {
            Query query = new Query(kind);
            query.setFilter(FilterOperator.EQUAL.of(property, null));
            PreparedQuery pQuery = datastore.prepare(query);
            results.addAll(pQuery.asList(FetchOptions.Builder.withDefaults()));
        } else {
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                // query in paralel
                List<List<Entity>> hs = new ArrayList<>();
                for (Object val : (ArrayList<?>) value) {
                    Query query = new Query(kind);
                    query.setFilter(FilterOperator.EQUAL.of(property, val));
                    PreparedQuery pQuery = datastore.prepare(query);
                    hs.add(pQuery.asList(FetchOptions.Builder.withDefaults()));
                }
                for (List<Entity> list : hs) {
                    results.addAll(list);
                }

            } else {
                Query query = new Query(kind);
                query.setFilter(FilterOperator.EQUAL.of(property, value));
                PreparedQuery pQuery = datastore.prepare(query);
                results = pQuery.asList(FetchOptions.Builder.withDefaults());
            }
        }
        removeDuplicates(results);
        filterDeletedEntities(results, request);
        return results;
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
                                if(options == null){
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
                            if(options == null){
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
                        if(options == null){
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
        Datastore datastore = new Datastore();
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



    public static String getInvalidCNP() {
        Datastore datastore = new Datastore();
        KeyRange autoId = datastore.allocateIds("InvalidCNP", 1);
        return Long.toString(autoId.getStart().getId());
    }

    public static void setStudentActiv(Entity student) {
        Datastore datastore = new Datastore();
        NamespaceManager.set("");
        Key keyAnUniversitar = getAncestor(student, "AnUniversitar");
        String namePersoana = student.getKey().getName();
        String nameStudentActiv = keyAnUniversitar.getId() + namePersoana;
        Entity studentActiv = new Entity("StudentActiv", nameStudentActiv);
        studentActiv.setProperty("keyPersoana", KeyFactory.createKey("Persoana", namePersoana));
        studentActiv.setProperty("keyAnUniversitar", keyAnUniversitar);
        datastore.put(studentActiv);
    }

    @SuppressWarnings("unchecked")
    public static void importFeedback(BlobKey blobKey, String importType, Object rowNr, String mesaj) {

        try {
            if (rowNr instanceof Integer) {
                Utils.saveFeedBack(blobKey, importType, rowNr, mesaj);
            } else {
                Utils.saveFeedBack(blobKey, importType, Integer.parseInt((String) rowNr), mesaj);
            }
        } catch (NumberFormatException alt_e) {
            try {
                Key keyEntity = KeyFactory.stringToKey((String) rowNr);
                if (!keyEntity.getKind().contains("Detalii")) {
                    keyEntity = KeyFactory.createKey(keyEntity, "Detalii" + keyEntity.getKind(), "0");
                }
                Datastore datastore = new Datastore();
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
                Utils.saveFeedBack(blobKey, importType, rowNr, mesaj);
            }
        }
    }

    public static void saveFeedBack(BlobKey blobKey, String importType, Object rowNr, String mesaj) {
        Datastore datastore = new Datastore();
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

    public static void checkImportFormat(String file, boolean checkPipe) throws InvalidField {
        if (file.split("[\r\n]+").length == 1) {
            throw new InvalidField("admin.import.backend.fileFormatException");
        }
        if (checkPipe && (file.split("|").length == 1)) {
            throw new InvalidField("admin.import.backend.fileFormatException");
        }
    }

    public static String getAppId() {
        String result = ApiProxy.getCurrentEnvironment().getAppId();
        return result.substring(result.indexOf("~") + 1);
    }

    public static void isMandatory(String mandatory, Object... args) throws InvalidField {
        for (Object arg : args) {
            if ((arg == null) || arg.equals("")) {
                throw new InvalidField("admin.backend.mandatoryParams", "fields", mandatory);
            }
        }
    }

    public static String getServerVersion() {
        return SystemProperty.applicationVersion.get();
    }

    public static void createUploadSesion(String hash, BlobKey blobKey, BlobInfo blobInfo, int rowNr, String type, HttpServletRequest req) {
        Datastore datastore = new Datastore();
        NamespaceManager.set("");
        Entity uploadSesion = new Entity("UploadSession");
        uploadSesion.setProperty("user", getCurrentEmail(req));
        uploadSesion.setProperty("date", blobInfo.getCreation());
        uploadSesion.setProperty("noRows", (rowNr - 2));
        uploadSesion.setProperty("blobKey", blobKey);
        uploadSesion.setProperty("hash", hash);
        uploadSesion.setProperty("type", type);
        datastore.put(uploadSesion);
    }

    public static void filterEntityProperties(Entity student, String json) {
        List<LinkedHashMap<String, String>> fields = extractFields(json);
        List<String> jsonProps = new ArrayList<>();
        for (LinkedHashMap<String, String> element : fields) {
            for (String attr : element.keySet()) {
                if ("name".equals(attr)) {
                    jsonProps.add(element.get(attr).replace("[]", ""));
                }
            }
        }
        Map<String, Object> props = student.getProperties();
        for (String prop : props.keySet()) {
            if (!jsonProps.contains(prop)) {
                student.removeProperty(prop);
            }
        }
    }

    public static String getLanguageCookie(HttpServletRequest req) {
        String lng = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals("i18next")) {
                    lng = c.getValue();
                    break;
                }
            }
        }
        return lng;
    }


}
