package ro.google.appenigne.request;

import javax.servlet.http.HttpServletRequest;

public class FilteredRequest extends TrimRequest {

    public FilteredRequest(HttpServletRequest request) {
        super(request);
    }

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return super.sanitize(input.replace("<", "&lt;").replace(">", "&gt;"));
    }
}
