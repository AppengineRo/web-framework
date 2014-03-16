package ro.appenigne.web.framework.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class FilteredRequest extends HttpServletRequestWrapper {

    public FilteredRequest(HttpServletRequest request) {
        super(request);
    }

    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    @Override
    public String getParameter(String paramName) {
        String value = super.getParameter(paramName);
        return this.sanitize(value);
    }

    /**
     * Ignores the leading and trailing spaces from values
     *
     */
    @Override
    public String[] getParameterValues(String paramName) {
        String[] parameterValues = super.getParameterValues(paramName);
        if(parameterValues!=null){
            for(int i=0;i<parameterValues.length;i++){
                parameterValues[i] = sanitize(parameterValues[i]);
            }
        }
        return parameterValues;
    }
}
