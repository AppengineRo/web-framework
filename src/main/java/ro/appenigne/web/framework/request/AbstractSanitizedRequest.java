package ro.appenigne.web.framework.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public abstract class AbstractSanitizedRequest extends HttpServletRequestWrapper {
    public AbstractSanitizedRequest(HttpServletRequest request) {
        super(request);
    }
    protected abstract String sanitize(String input);

    @Override
    public String getParameter(String paramName) {
        String value = super.getParameter(paramName);
        if (value == null) {
            if (paramName.contains("[]")) {
                value = super.getParameter(paramName.replace("[]", ""));
            } else {
                value = super.getParameter(paramName + "[]");
            }
        }
        return this.sanitize(value);
    }

    @Override
    public String[] getParameterValues(String paramName) {
        String[] values;
        String[] values2;
        if (paramName.contains("[]")) {
            values = super.getParameterValues(paramName);
            values2 = super.getParameterValues(paramName.replace("[]", ""));
        } else {
            values = super.getParameterValues(paramName + "[]");
            values2 = super.getParameterValues(paramName);
        }
        if (values != null) {
            for (int index = 0; index < values.length; index++) {
                values[index] = this.sanitize(values[index]);
            }
            return values;
        } else if (values2 != null) {
            for (int index = 0; index < values2.length; index++) {
                values2[index] = this.sanitize(values2[index]);
            }
            return values2;
        } else {
            return null;
        }
    }
}
