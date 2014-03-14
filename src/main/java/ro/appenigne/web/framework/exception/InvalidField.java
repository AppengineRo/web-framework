package ro.appenigne.web.framework.exception;


import ro.appenigne.web.framework.utils.GsonUtils;

import java.util.LinkedHashMap;

public class InvalidField extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidField() {
        super();
    }

    public InvalidField(String message) {
        super(message);
    }

    public InvalidField(String message, Object... args) {
        super(i18nObj(message, args));
    }

    private static String i18nObj(String message, Object[] args) {
        LinkedHashMap<String, Object> i18n = new LinkedHashMap<>();
        i18n.put("text", message);
        int end = args.length;
        if (args.length % 2 > 0) {
            end--;
        }
        if (end > 0) {
            LinkedHashMap<String, Object> vars = new LinkedHashMap<>();
            for (int i = 0; i < end; i += 2) {
                vars.put(args[i].toString(), args[i + 1]);
            }
            i18n.put("vars", vars);
        }
        return GsonUtils.getGson().toJson(i18n);
    }

    public static String i18n(String message, Object... args) {
        return i18nObj(message, args);
    }

    public InvalidField(Throwable cause) {
        super(cause);
    }

    public InvalidField(String message, Throwable cause) {
        super(message, cause);
    }

}
