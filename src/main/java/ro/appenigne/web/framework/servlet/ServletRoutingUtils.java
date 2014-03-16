package ro.appenigne.web.framework.servlet;

import ro.appenigne.web.framework.request.AlterableRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class ServletRoutingUtils {
    private static final AtomicReference<LinkedHashMap<String[], String>> urlPatterns = new AtomicReference<>();
    private static final String PREFIX = "url:";

    public static Object getController(AlterableRequest request) throws UnsupportedEncodingException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (urlPatterns.get() == null) {
            writeUrlPatterns();
        }
        String[] requestUriParts = parseUrlPattern(request.getRequestURI());

        LinkedHashMap<String[], String> urlPatternMap = urlPatterns.get();
        for (int i = requestUriParts.length; i >= 0; i--) {
            for (Map.Entry<String[], String> urlPatternEntry : urlPatternMap.entrySet()) {
                String[] urlPattern = urlPatternEntry.getKey();
                if (urlPattern.length != i) {
                    continue;
                }
                boolean isGood = true;
                for (int j = 0; j < urlPattern.length; j++) {
                    if (!isVariable(urlPattern[j]) && !urlPattern[j].equals(requestUriParts[j])) {
                        isGood = false;
                        break;
                    }
                }
                if (isGood) {
                    for (int j = 0; j < urlPattern.length; j++) {
                        if (isVariable(urlPattern[j])) {
                            request.addParam(getVariableName(urlPattern[j]), requestUriParts[j]);
                        }
                    }
                    Class<?> controllerClass = Class.forName(urlPatternEntry.getValue());
                    return controllerClass.newInstance();
                }
            }
        }
        return null;
    }

    private static boolean isVariable(String str) {
        return str.startsWith("{") && str.endsWith("}");
    }

    private static String getVariableName(String varName) {
        return varName.substring(1, varName.length() - 1);
    }

    private static String[] parseUrlPattern(String urlPattern) throws UnsupportedEncodingException {
        urlPattern = urlPattern.replaceAll("[*]", "").replaceAll("[/]{2,}", "/").replaceFirst("^/", "").replaceFirst("/$", "");
        String[] split = urlPattern.split("[/]");
        int startIndex = 0;
        int endIndex = 0;
        for (int i = 0; i < split.length; i++) {
            split[i] = URLDecoder.decode(split[i], "UTF-8").trim();
            if (i == startIndex && split[i].isEmpty()) {
                startIndex++;
                endIndex++;
            }
            if (i == endIndex && !split[i].isEmpty()) {
                endIndex++;
            }
        }
        if (startIndex != 0 || endIndex != split.length) {
            split = Arrays.copyOfRange(split, startIndex, endIndex);
        }
        return split;
    }

    private static synchronized void writeUrlPatterns() throws UnsupportedEncodingException {
        Properties properties = System.getProperties();
        LinkedHashMap<String[], String> newUrlPatterns = new LinkedHashMap<>();
        for (String propName : properties.stringPropertyNames()) {
            if (propName.startsWith(PREFIX)) {
                newUrlPatterns.put(parseUrlPattern(propName.substring(PREFIX.length())), System.getProperty(propName));
            }
        }
        urlPatterns.compareAndSet(null, newUrlPatterns);
    }


}
