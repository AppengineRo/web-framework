package ro.appenigne.web.framework.utils;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EntityUtils {

    /**
     * Returns a list with the Strings found<br />
     * Does not return null
     */
    public static List<String> getAsStringList(Entity entity, String prop) {
        List<String> initialList = getAsList(entity, prop, String.class);
        List<String> notEmptyList = new ArrayList<>();
        for(String s:initialList){
            if(!s.isEmpty()){
                notEmptyList.add(s);
            }
        }
        return notEmptyList;
    }

    /**
     * Returns a list with the Longs found<br />
     * Does not return null
     */
    public static List<Long> getAsLongList(Entity entity, String prop) {
        return getAsList(entity, prop, Long.class);
    }

    /**
     * Returns a list with the Keys found<br />
     * Does not return null
     */
    public static List<Key> getAsKeyList(Entity entity, String prop) {
        return getAsList(entity, prop, Key.class);
    }

    /**
     * Returns a set with the Strings found<br />
     * Does not return null
     */
    public static Set<String> getAsStringSet(Entity entity, String prop) {
        Set<String> initialList = getAsSet(entity, prop, String.class);
        Set<String> notEmptyList = new LinkedHashSet<>();
        for(String s:initialList){
            if(!s.isEmpty()){
                notEmptyList.add(s);
            }
        }
        return notEmptyList;
    }

    /**
     * Returns a set with the Longs found<br />
     * Does not return null
     */
    public static Set<Long> getAsLongSet(Entity entity, String prop) {
        return getAsSet(entity, prop, Long.class);
    }

    /**
     * Returns a set with the Keys found<br />
     * Does not return null
     */
    public static Set<Key> getAsKeySet(Entity entity, String prop) {
        return getAsSet(entity, prop, Key.class);
    }

    public static <T> List<T> getAsList(Entity entity, String prop, Class<T> type) {
        List<T> list = new ArrayList<>();
        if (entity != null) {
            if (type.isInstance(entity.getProperty(prop))) {
                T cast = type.cast(entity.getProperty(prop));
                list.add(cast);
            } else if (entity.getProperty(prop) instanceof List<?>) {
                for (Object val : (List<?>) entity.getProperty(prop)) {
                    if (type.isInstance(val)) {
                        T cast = type.cast(val);
                        list.add(cast);
                    }
                }
            } else if (entity.getProperty(prop) instanceof Set<?>) {
                for (Object val : (Set<?>) entity.getProperty(prop)) {
                    if (val instanceof Key) {
                        T cast = type.cast(val);
                        list.add(cast);
                    }
                }
            }
        }
        return list;
    }
    public static <T> Set<T> getAsSet(Entity entity, String prop, Class<T> type) {
        Set<T> list = new LinkedHashSet<>();
        if (entity != null) {
            if (type.isInstance(entity.getProperty(prop))) {
                T cast = type.cast(entity.getProperty(prop));
                list.add(cast);
            } else if (entity.getProperty(prop) instanceof List<?>) {
                for (Object val : (List<?>) entity.getProperty(prop)) {
                    if (type.isInstance(val)) {
                        T cast = type.cast(val);
                        list.add(cast);
                    }
                }
            } else if (entity.getProperty(prop) instanceof Set<?>) {
                for (Object val : (Set<?>) entity.getProperty(prop)) {
                    if (val instanceof Key) {
                        T cast = type.cast(val);
                        list.add(cast);
                    }
                }
            }
        }
        return list;
    }

}
