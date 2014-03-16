package ro.appenigne.web.framework.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class IgnoringArrayLikeParamNameRequest extends HttpServletRequestWrapper {
    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public IgnoringArrayLikeParamNameRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * Ignores the ending square brackets from the paramName
     *
     */
    @Override
    public String[] getParameterValues(String paramName) {
        String[] values;
        String[] values2;
        if (paramName.endsWith("[]")) {
            values = super.getParameterValues(paramName);
            values2 = super.getParameterValues(paramName.substring(0, paramName.length()-2));
        } else {
            values = super.getParameterValues(paramName + "[]");
            values2 = super.getParameterValues(paramName);
        }
        if(values!= null && values2!=null){
            //TODO: create a new array with both values
            return values;
        }else if (values != null) {
            return values;
        } else if (values2 != null) {
            return values2;
        } else {
            return null;
        }
    }

    /**
     * Ignores the ending square brackets from the paramName
     *
     */
    @Override
    public String getParameter(String paramName) {
        String value = super.getParameter(paramName);
        if (value == null) {
            if (paramName.endsWith("[]")) {
                value = super.getParameter(paramName.substring(0, paramName.length()-2));
            } else {
                value = super.getParameter(paramName + "[]");
            }
        }
        return value;
    }
}
