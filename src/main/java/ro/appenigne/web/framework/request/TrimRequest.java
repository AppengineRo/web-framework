package ro.appenigne.web.framework.request;

import javax.servlet.http.HttpServletRequest;

public class TrimRequest extends AbstractSanitizedRequest {
    public TrimRequest(HttpServletRequest request) {
        super(request);
    }

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()/*.replaceAll("[ ]{2,}", " ")*/;
    }

}
