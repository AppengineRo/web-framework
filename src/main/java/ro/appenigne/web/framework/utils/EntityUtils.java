package ro.appenigne.web.framework.utils;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.users.User;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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

    /**
     * Returns the value as a string<br />
     * Does not return null
     */
    public static String getAsString(Entity entity, String prop) {
        Object o = entity.getProperty(prop);
        return getAsString(o);
    }
    /**
     * Returns the value as a string<br />
     * Does not return null
     */
    public static String getAsString(Object o) {
        if(o==null){
            return "";
        }
        if(o instanceof Date){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(((Date) o));
        } else if(o instanceof String){
            return (String) o;
        } else if(o instanceof User){
            return GsonUtils.getGson().toJson(o);
        } else if(o instanceof Text){
            return ((Text) o).getValue();
        } else if(o instanceof Integer){
            return String.valueOf(o);
        } else if(o instanceof Double){
            DecimalFormat nf = new DecimalFormat("0.####");
            return nf.format(o);
        } else if(o instanceof Long){
            return ((Long) o)+"";
        } else if(o instanceof List){
            List lo=((List) o);
            List<String> l=new ArrayList<>();
            for(Object o2:lo){
                l.add(getAsString(o2));
            }
            return StringUtils.join(",", l);
        }
        return String.valueOf(o);
    }
}
