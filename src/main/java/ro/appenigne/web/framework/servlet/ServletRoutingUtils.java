package ro.appenigne.web.framework.servlet;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class ServletRoutingUtils {
    private static final AtomicReference<LinkedHashMap<String[], String>> urlPatterns = new AtomicReference<>();
    private static final String PREFIX = "url:";

    public static Object getController(HttpServletRequest request) throws UnsupportedEncodingException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (urlPatterns.get() == null) {
            writeUrlPatterns();
        }
        String[] folders = parseUrlPattern(request.getRequestURI());
        //System.out.println(Arrays.toString(folders));

        LinkedHashMap<String[], String> urlPatternMap = urlPatterns.get();
        for (int j = folders.length; j >= 0; j--) {
            for (Map.Entry<String[], String> urlPatternEntry : urlPatternMap.entrySet()) {
                String[] urlPattern = urlPatternEntry.getKey();

                //System.out.println("scanning: " + urlPatternEntry.getValue() + "   " + Arrays.toString(urlPattern) + " " + urlPattern.length + " " + folders.length);
                if (urlPattern.length != j) {
                    continue;
                }
                boolean isGood = true;
                for (int i = 0; i < urlPattern.length; i++) {
                    if (!isVariable(urlPattern[i]) && !urlPattern[i].equals(folders[i])) {
                        isGood = false;
                        break;
                    }
                }
                if (isGood) {
                    System.out.println("found: " + urlPatternEntry.getValue());
                    System.out.println("request: " + Arrays.toString(folders));
                    System.out.println("urlPattern: " + Arrays.toString(urlPattern));
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
            //System.out.print("remove empty strings");
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
