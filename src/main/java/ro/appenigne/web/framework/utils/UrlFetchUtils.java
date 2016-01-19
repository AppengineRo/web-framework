package ro.appenigne.web.framework.utils;

import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.users.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;

public class UrlFetchUtils {
	public static String makePost(String urlParam, Map<String, String[]> params) throws IOException {
		URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
		URL url = new URL(urlParam);
		HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST, com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60d));
		request.setPayload(urlEncodeUTF8(params).getBytes());
		HTTPResponse response = fetcher.fetch(request);
		if (response.getResponseCode() == 200) {
			return new String(response.getContent());
		}
		return null;
	}
	public static String makeGet(String urlParam, Map<String, String[]> params) throws IOException {
		URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
		String urlParamWithParams = urlParam;
		if(!urlParamWithParams.contains("?")) {
			urlParamWithParams+="?";
		} else {
			urlParamWithParams+="&";
		}
		urlParamWithParams += urlEncodeUTF8(params);
		URL url = new URL(urlParamWithParams);
		Log.d("Fetching " + urlParamWithParams);
		HTTPRequest request = new HTTPRequest(url, HTTPMethod.GET, com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60d));
		HTTPResponse response = fetcher.fetch(request);
		if (response.getResponseCode() == 200) {
			return new String(response.getContent());
		}
		return null;
	}
	public static Future<HTTPResponse> makeAsyncGet(String urlParam, Map<String, String[]> params) throws IOException {
		URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
		String urlParamWithParams = urlParam;
		if(!urlParamWithParams.contains("?")) {
			urlParamWithParams+="?";
		} else {
			urlParamWithParams+="&";
		}
		urlParamWithParams += urlEncodeUTF8(params);
		URL url = new URL(urlParamWithParams);
		HTTPRequest request = new HTTPRequest(url, HTTPMethod.GET, com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60d));
		return fetcher.fetchAsync(request);
	}
	public static Future<HTTPResponse> makeAsyncPost(String urlParam, Map<String, String[]> params) throws IOException {
		URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
		URL url = new URL(urlParam);
		HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST, com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60d));
		request.setPayload(urlEncodeUTF8(params).getBytes());
		return fetcher.fetchAsync(request);
	}
	public static String[] getAsString(Object o){
		if(o instanceof Date){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return new String[]{sdf.format(((Date) o))};
		} else if(o instanceof String){
			return new String[]{(String) o};
		} else if(o instanceof User){
			return new String[]{GsonUtils.getGson().toJson(o)};
		} else if(o instanceof Text){
			return new String[]{((Text) o).getValue()};
		} else if(o instanceof Integer){
			return new String[]{((Integer) o)+""};
		} else if(o instanceof Double){
			return new String[]{((Double) o)+""};
		} else if(o instanceof Long){
			return new String[]{((Long) o)+""};
		} else if(o instanceof List){
			List<?> l=(List<?>) o;
			String[] returnValue = new String[l.size()];
			for (int i = 0; i < l.size(); i++) {
				if (l.get(i) instanceof Date) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					returnValue[i] = sdf.format(((Date) l.get(i)));
				} else if (l.get(i) instanceof String) {
					returnValue[i] = (String) l.get(i);
				} else if (l.get(i) instanceof User) {
					returnValue[i] = ((User) l.get(i)).getEmail();
				} else if (l.get(i) instanceof Text) {
					returnValue[i] = ((Text) l.get(i)).getValue();
				} else if (l.get(i) instanceof Integer) {
					returnValue[i] = ((Integer) l.get(i)) + "";
				} else if (l.get(i) instanceof Double) {
					returnValue[i] = ((Double) l.get(i)) + "";
				} else if (l.get(i) instanceof Long) {
					returnValue[i] = ((Long) l.get(i)) + "";
				}
			}
			return returnValue;
		}
		return new String[]{""};
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
        /*
        UserService userService = UserServiceFactory.getUserService();
        if (logType != null && destinationUrl != null) {

            if (!destinationUrl.startsWith("/")) {
                destinationUrl = "/" + destinationUrl;
            }

            if (logType.equals("login")) {
                return userService.createLoginURL(destinationUrl);
            } else if (logType.equals("logout")) {
                return userService.createLogoutURL(destinationUrl);
            }
        }
        return null;*/
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
