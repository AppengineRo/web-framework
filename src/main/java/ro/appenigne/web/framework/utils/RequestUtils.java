package ro.appenigne.web.framework.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class RequestUtils {
    public static String getNaketUrl(HttpServletRequest request){
        //starts with http:// or https://
        return request.getRequestURL().substring(0, request.getRequestURL().indexOf("/", 8));
    }
    public static String insertRemoveParam(HttpServletRequest req, String key, String value, String... keysToRemove) {
        LinkedHashMap<String, String[]> newParameterMap = getQueryParams(req);

        if (newParameterMap.get(key) != null) {
            String[] oldValue = newParameterMap.get(key);
            String[] newValue = new String[oldValue.length + 1];
            System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
            newValue[newValue.length - 1] = value;
            newParameterMap.put(key, newValue);
        } else {
            newParameterMap.put(key, new String[]{value});
        }
        for (String keyToRemove : keysToRemove) {
            newParameterMap.remove(keyToRemove);
        }
        return urlEncodeUTF8(newParameterMap);
    }

    public static String insertParam(HttpServletRequest req, String key, String value) {
        LinkedHashMap<String, String[]> newParameterMap = getQueryParams(req);

        if (newParameterMap.get(key) != null) {
            String[] oldValue = newParameterMap.get(key);
            String[] newValue = new String[oldValue.length + 1];
            System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
            newValue[newValue.length - 1] = value;
            newParameterMap.put(key, newValue);
        } else {
            newParameterMap.put(key, new String[]{value});
        }
        return urlEncodeUTF8(newParameterMap);
    }

    public static String removeParam(HttpServletRequest req, String... keysToRemove) {
        LinkedHashMap<String, String[]> newParameterMap = getQueryParams(req);

        for (String keyToRemove : keysToRemove) {
            newParameterMap.remove(keyToRemove);
        }
        return urlEncodeUTF8(newParameterMap);
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String urlEncodeUTF8(Map<String, String[]> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            boolean addAnd = false;
            for (String val : entry.getValue()) {
                if (val == null) {
                    continue;
                }
                if(addAnd){
                    sb.append("&");
                }
                sb.append(urlEncodeUTF8(entry.getKey())).append("=").append(urlEncodeUTF8(val));
                addAnd = true;
            }
        }
        return sb.toString();
    }

    public static String getLogUrl(String logType, String destinationUrl) {
        return "/disconnect?redirect_to="+destinationUrl;
    }

    public static LinkedHashMap<String, String[]> getQueryParams(HttpServletRequest req) {
        String queryString = getFullURL(req);
        return getQueryParams(queryString);
    }

    public static LinkedHashMap<String, String[]> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            LinkedHashMap<String, String[]> paramsWithArr = new LinkedHashMap<>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
                for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                    List<String> value = entry.getValue();
                    String[] strings = value.toArray(new String[value.size()]);
                    paramsWithArr.put(entry.getKey(), strings);
                }
            }

            return paramsWithArr;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    public static String getFullURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }
}
