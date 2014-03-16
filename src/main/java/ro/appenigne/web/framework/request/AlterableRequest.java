package ro.appenigne.web.framework.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.lang.reflect.Array;
import java.util.LinkedHashMap;

public class AlterableRequest extends HttpServletRequestWrapper {

    private LinkedHashMap<String, String[]> auxiliaryParams = new LinkedHashMap<>();

    /**
     * Constructs a request object wrapping the given request.
     *
     * @throws IllegalArgumentException if the request is null
     */
    public AlterableRequest(HttpServletRequest request) {
        super(request);
    }

    public void setParam(String paramName, String... paramValues) {
        auxiliaryParams.put(paramName, paramValues);
    }

    public void addParam(String paramName, String... paramValues) {
        String[] strings = concatenate(auxiliaryParams.get(paramName), paramValues);
        auxiliaryParams.put(paramName, strings);
    }


    @Override
    public String getParameter(String name) {
        String parameterValue = super.getParameter(name);
        if (parameterValue == null) {
            String[] auxiliaryValues = auxiliaryParams.get(name);
            if (auxiliaryValues != null && auxiliaryValues.length > 0)
                return auxiliaryValues[0];
        }
        return parameterValue;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] parameterValues = super.getParameterValues(name);
        if (parameterValues == null) {
            return auxiliaryParams.get(name);
        }
        return parameterValues;
    }

    public <T> T[] concatenate(T[] a, T[] b) {
        if (a == null || a.length == 0) {
            return b;
        }
        if (b == null || b.length == 0) {
            return a;
        }
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
