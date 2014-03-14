package ro.appenigne.web.framework.form;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.gson.Gson;
import ro.appenigne.web.framework.datastore.Datastore;
import ro.appenigne.web.framework.exception.InvalidField;
import ro.appenigne.web.framework.utils.GsonUtils;
import ro.appenigne.web.framework.utils.Utils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("unchecked")
public class FormValidate {
    private enum ValidateFunctions {
        required("{\"text\":\"validator.backend.required\",\"vars\":{\"name\":\"%s\"}}"),
        unique("{\"text\":\"validator.backend.unique\",\"vars\":{\"name\":\"%s\"}}"),
        minlength("{\"text\":\"validator.backend.minlength\",\"vars\":{\"name\":\"%s\",\"count\":\"%.0f\"}}"),
        maxlength("{\"text\":\"validator.backend.maxlength\",\"vars\":{\"name\":\"%s\",\"count\":\"%.0f\"}}"),
        rangelength("{\"text\":\"validator.backend.rangelength\",\"vars\":{\"name\":\"%s\",\"min\":\"%.0f\",\"max\":\"%.0f\"}}"),
        number("{\"text\":\"validator.backend.number\",\"vars\":{\"name\":\"%s\"}}"),
        date("{\"text\":\"validator.backend.date\",\"vars\":{\"name\":\"%s\"}}"),
        checkFloat("{\"text\":\"validator.backend.checkFloat\",\"vars\":{\"name\":\"%s\"}}"),
        min("{\"text\":\"validator.backend.min\",\"vars\":{\"name\":\"%s\",\"min\":\"%.2f\"}}"),
        max("{\"text\":\"validator.backend.max\",\"vars\":{\"name\":\"%s\",\"max\":\"%.2f\"}}"),
        range("{\"text\":\"validator.backend.range\",\"vars\":{\"name\":\"%s\",\"min\":\"%.2f\",\"max\":\"%.2f\"}}"),
        email("{\"text\":\"validator.backend.email\",\"vars\":{\"name\":\"%s\",\"email\":\"%s\"}}"),
        url("{\"text\":\"validator.backend.url\",\"vars\":{\"name\":\"%s\"}}"),
        equalTo("{\"text\":\"validator.backend.equalTo\",\"vars\":{\"name\":\"%s\"}}"),
        checkCnp("{\"text\":\"validator.backend.checkCnp\",\"vars\":{\"name\":\"%s\"}}"),
        fieldValueDependency("{\"text\":\"validator.backend.fieldValueDependency\",\"vars\":{\"name\":\"%s\"}}"),
        geoPt("Campul %s trebuie sa aiba forma specificata! "),
        geoPtList("Campul %s trebuie sa aiba forma specificata! ");
        private final String mesaj;

        ValidateFunctions(String str) {
            this.mesaj = str;
        }

        public String getMesaj() {
            return this.mesaj;
        }

    }

    public static void validate(String formJson, HttpServletRequest req, Key keyEntity) throws InvalidField {
        Gson gson = GsonUtils.getGson();
        LinkedHashMap<String, Object> form = (LinkedHashMap<String, Object>) gson.fromJson(formJson, Object.class);

        ArrayList<Object> formElements = (ArrayList<Object>) form.get("elements");
        List<LinkedHashMap<String, Object>> elements = getFields(formElements);
        StringBuilder errors = new StringBuilder();
        for (LinkedHashMap<String, Object> element : elements) {
            if (element.get("validate") != null) {
                LinkedHashMap<String, Object> conditii = (LinkedHashMap<String, Object>) element.get("validate");
                if (conditii.containsKey("required") && conditii.get("required").equals(true)) {
                    if (!required(req.getParameterValues((String) element.get("name")), (Boolean) conditii.get("required"))) {
                        errors.append(String.format(ValidateFunctions.valueOf("required").getMesaj(),
                                (String) element.get("htmlCaption")));
                    }
                }
                if ((req.getParameter((String) element.get("name")) != null)
                        && !req.getParameter((String) element.get("name")).isEmpty()) {
                    for (Entry<String, Object> conditie : conditii.entrySet()) {
                        ValidateFunctions validateFunctions = ValidateFunctions.valueOf(conditie.getKey());
                        switch (validateFunctions) {
                            case required:
                                break;
                            case unique:
                                if (!unique(element, req, keyEntity)) {
                                    errors.append(String.format(validateFunctions.getMesaj(), element.get("htmlCaption"), conditie.getValue()));
                                }
                                break;
                            case minlength:
                                if (!minlength(req.getParameter((String) element.get("name")),
                                        (Double) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            conditie.getValue()));
                                }
                                break;
                            case maxlength:
                                if (!maxlength(req.getParameter((String) element.get("name")),
                                        (Double) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            conditie.getValue()));
                                }
                                break;
                            case rangelength:
                                if (!rangelength(req.getParameter((String) element.get("name")),
                                        (Object[]) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            (((Object[]) conditie.getValue())[0]),
                                            (((Object[]) conditie.getValue())[1])));
                                }
                                break;
                            case number:
                                if (!number(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case date:
                                if (!date(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case checkFloat:
                                if (!checkFloat(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case min:
                                if (!min(req.getParameter((String) element.get("name")), (Double) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            conditie.getValue()));
                                }
                                break;
                            case max:
                                if (!max(req.getParameter((String) element.get("name")), (Double) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            conditie.getValue()));
                                }
                                break;
                            case range:
                                if (!range(req.getParameter((String) element.get("name")),
                                        (Object[]) conditie.getValue())) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            element.get("htmlCaption"),
                                            (((Object[]) conditie.getValue())[0]),
                                            (((Object[]) conditie.getValue())[1])));
                                }
                                break;
                            case email:
                                String elName = (String) element.get("name");
                                for (String param : req.getParameterValues(elName)) {
                                    if (!param.trim().isEmpty()) {
                                        if (!email(param)) {
                                            errors.append(String.format(validateFunctions.getMesaj(),
                                                    element.get("htmlCaption"), param));
                                        }
                                    }
                                }
                                break;
                            case url:
                                if (!url(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case equalTo:
                                if (!equalTo(req.getParameter((String) element.get("name")),
                                        (String) conditie.getValue(),
                                        req)) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("name")));
                                }
                                break;
                            case checkCnp:
                                if (!checkCnp(req.getParameter((String) element.get("name")),
                                        Utils.getCodTara(req.getParameter("tara")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case fieldValueDependency:
                                if (!fieldValueDependency(req.getParameter((String) element.get("name")),
                                        conditie.getValue(),
                                        req)) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case geoPt:
                                if (!geoPt(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            case geoPtList:
                                if (!geoPtList(req.getParameter((String) element.get("name")))) {
                                    errors.append(String.format(validateFunctions.getMesaj(),
                                            (String) element.get("htmlCaption")));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        if (errors.length() != 0) {
            throw new InvalidField(errors.toString());
        }
    }


    public static List<LinkedHashMap<String, Object>> getFields(ArrayList<Object> formElements, String fieldset) {
        List<LinkedHashMap<String, Object>> elements = new ArrayList<>();
        for (Object formElement : formElements) {
            LinkedHashMap<String, Object> formEl = (LinkedHashMap<String, Object>) formElement;
            if ((formEl.get("elements") == null) || (formEl.get("template") != null)) {
                formEl.put("fieldset", fieldset);
                elements.add(formEl);
            }
            if ((formEl.get("elements") != null) && (formEl.get("template") == null)) {
                if (formEl.get("permisiuni") != null) {
                    for (LinkedHashMap<String, Object> el : (ArrayList<LinkedHashMap<String, Object>>) formEl.get("elements")) {
                        el.put("permisiuni", formEl.get("permisiuni"));
                    }
                }
                if ((formEl.get("type") != null) && formEl.get("type").equals("fieldset")) {
                    elements.addAll(getFields((ArrayList<Object>) formEl.get("elements"), (String) formEl.get("caption")));
                } else {
                    elements.addAll(getFields((ArrayList<Object>) formEl.get("elements"), fieldset));
                }
            }
        }
        return elements;
    }

    public static List<LinkedHashMap<String, Object>> getFields(ArrayList<Object> formElements) {
        return getFields(formElements, null);
    }

    public static boolean required(String[] str, boolean isRequired) {
        if (!isRequired) {
            return true;
        }
        if (str == null) {
            return true;
        }
        for (String s : str) {
            if (!s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean unique(LinkedHashMap<String, Object> element, HttpServletRequest req, Key keyEntity) throws InvalidField {
        String kind;
        String kindDetalii;
        Key key;
        Key keyDetalii;
        if (keyEntity != null) {
            if (keyEntity.getKind().contains("Detalii")) {
                kind = keyEntity.getParent().getKind();
                kindDetalii = keyEntity.getKind();
                key = keyEntity.getParent();
                keyDetalii = keyEntity;
            } else {
                kind = keyEntity.getKind();
                kindDetalii = "Detalii" + kind;
                key = keyEntity;
                keyDetalii = KeyFactory.createKey(key, kindDetalii, "0");
            }
            Object value = JsonUtils.getObject(req, null, null, element);
            if (queryUnique(kindDetalii, keyDetalii, (String) element.get("name"), value)) {
                if (!queryUnique(kind, key, (String) element.get("name"), value)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean queryUnique(String kind, Key key, String name, Object value) {
        Datastore datastore = new Datastore();
        NamespaceManager.set(key.getNamespace());
        Query q = new Query(kind);
        q.setFilter(FilterOperator.EQUAL.of(name, value));
        PreparedQuery pq = datastore.prepare(q);
        List<Entity> list = pq.asList(FetchOptions.Builder.withDefaults());
        NamespaceManager.set("");
        if (list != null && !list.isEmpty()) {
            for (Entity e : list) {
                if (key.equals(e.getKey())) {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean required(String str) {
        return str == null || !str.isEmpty();
    }

    public static boolean minlength(String str, double minlength) {
        return str.trim().length() >= minlength;
    }

    public static boolean maxlength(String str, double maxlength) {
        return str.trim().length() <= maxlength;
    }

    public static boolean rangelength(String str, Double minlength, Double maxlength) {
        return minlength(str, minlength) && maxlength(str, maxlength);
    }

    public static boolean rangelength(String str, Object[] objects) {
        return minlength(str, (Double) objects[0]) && maxlength(str, (Double) objects[1]);
    }

    public static boolean number(String str) {
        if (!str.trim().isEmpty()) {
            try {
                Double.parseDouble(str.replace(",", "."));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean date(String str) {
        if (!str.trim().isEmpty()) {
            try {
                new Date(Long.parseLong(str.trim()));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkFloat(String str) {
        if (!str.trim().isEmpty()) {
            str = str.replaceAll(",", ".");
            try {
                Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean min(String str, double min) {
        return number(str) && !(!str.trim().isEmpty() && (Double.parseDouble(str) < min));
    }

    public static boolean max(String str, double max) {
        return number(str) && !(!str.isEmpty() && (Double.parseDouble(str) > max));

    }

    public static boolean range(String str, double min, double max) {
        return min(str, min) && max(str, max);
    }

    public static boolean range(String str, Object[] objects) {
        return min(str, (Double) objects[0]) && max(str, (Double) objects[1]);
    }

    public static boolean email(String str) {
        try {
            new InternetAddress(str, true);
        } catch (AddressException e) {
            return false;
        }
        return true;
    }

    public static boolean url(String str) {
        try {
            new URL(str);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    public static boolean equalTo(String str, String str2, HttpServletRequest req) {
        return str.equals(req.getParameter(str2.replace("#", "")));
    }

    private static boolean geoPt(String geo) {
        ArrayList<String> geoVals = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(geo, ",");
        while (st.hasMoreElements()) {
            geoVals.add(st.nextToken());
        }

        if (geoVals.size() != 2) {
            return false;
        }

        Float latVal = Float.parseFloat(geoVals.get(0));
        Float longVal = Float.parseFloat(geoVals.get(1));
        try {
            GeoPt geoPoint = new GeoPt(latVal, longVal);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private static boolean geoPtList(String geo) {
        ArrayList<GeoPt> geoPointList = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(geo, ";");
        while (st.hasMoreElements()) {
            String geoVals = st.nextToken();
            StringTokenizer tok = new StringTokenizer(geoVals, ",");
            if (tok.countTokens() != 2) {
                return false;
            }
            Float latVal = Float.parseFloat(tok.nextToken());
            Float longVal = Float.parseFloat(tok.nextToken());
            try {
                geoPointList.add(new GeoPt(latVal, longVal));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<NIF> createNIFValidators() {

        ArrayList<NIF> countries = new ArrayList<>();
        countries.add(NIF.createAT());
        countries.add(NIF.createBE());
        countries.add(NIF.createBG());
        countries.add(NIF.createCY());
        countries.add(NIF.createCZ());
        countries.add(NIF.createCZ1());
        countries.add(NIF.createCZ2());
        countries.add(NIF.createDE());
        countries.add(NIF.createDK());
        countries.add(NIF.createEE());
        countries.add(NIF.createES());
        countries.add(NIF.createES1());
        //countries.add(createFI()); //eronat
        countries.add(NIF.createFR());
        countries.add(NIF.createFR1());
        countries.add(NIF.createGB());
        countries.add(NIF.createGB1());
        countries.add(NIF.createGR());
        countries.add(NIF.createGR1());
        countries.add(NIF.createHU());
        countries.add(NIF.createIE());
        countries.add(NIF.createIE1());
        countries.add(NIF.createIT());
        countries.add(NIF.createLT());
        countries.add(NIF.createLU());
        countries.add(NIF.createLV());
        countries.add(NIF.createMT());
        //countries.add(createNL());// eronat
        //countries.add(createPL());// eronat
        countries.add(NIF.createPT());
        countries.add(NIF.createRO());
        countries.add(NIF.createMD());
        countries.add(NIF.createSE());
        countries.add(NIF.createSI());
        countries.add(NIF.createSK());
        countries.add(NIF.createSK1());
        countries.add(NIF.createSK2());
        return countries;
    }

    public static boolean checkCnp(String cnp, String codTara) throws InvalidField {
        if (cnp.equals("necunoscut")) {
            return true;
        }

        if (codTara == null) {
            return false;
        }

        Boolean isValid = false;
        boolean countryFound = false;
        cnp = cnp.trim();
        ArrayList<NIF> countries = createNIFValidators();
        for (NIF c : countries) {
            if (c.name.equalsIgnoreCase(codTara)) {
                countryFound = true;
                if (cnp.matches(c.regex)) {
                    if (c.validate(cnp)) {
                        isValid = true;
                    }
                }
            }
        }
        if (!countryFound) {
            // Daca nu avem tara inseamna ca nu avem cum sa validam si returnam true
            isValid = true;
            /*
             * NIF romania = createRO();
			 * if(cnp.matches(romania.regex)){
			 * if (romania.validate(cnp)){
			 * isValid = true;
			 * }
			 * }
			 */
        }
        return isValid;
    }

    public static boolean fieldValueDependency(String str, Object obj, HttpServletRequest req) {
        LinkedHashMap<String, Object> objMap = (LinkedHashMap<String, Object>) obj;
        objMap.get("value");
        String parameter = req.getParameter(((String) objMap.get("selector")).replace("#", ""));
        if ((Boolean) objMap.get("equals")) {
            if (parameter != null && parameter.equals(objMap.get("value"))) {
                return required(str);
            } else {
                return true;
            }
        } else {
            if (parameter != null && !parameter.equals(objMap.get("value"))) {
                return required(str);
            } else {
                return true;
            }
        }
    }

    public static Double calculateField(LinkedHashMap<String, Object> element, Entity detalii, HttpServletRequest req) throws InvalidField {
        try {
            String formula = (String) element.get("formula");
            String nz = (String) element.get("numarZecimale");
            String az = (String) element.get("aproximareZecimale");
            if ((formula == null) || formula.isEmpty()) {
                return null;
            }
            formula = formula.replace(" ", "").replace(",", ".");
            String[] vals = formula.split("[+-/*()]");
            LinkedHashMap<String, Double> vars = new LinkedHashMap<>();
            for (String var : vals) {
                if ((var == null) || var.isEmpty()) {
                    continue;
                }
                if (var.matches("([a-zA-Z][a-zA-Z0-9]*)")) {
                    if (!vars.containsKey(var)) {
                        Double varDetalii = null;
                        if (detalii.getProperty(var) instanceof Double) {
                            varDetalii = (Double) detalii.getProperty(var);
                        }
                        if (detalii.getProperty(var) instanceof Long) {
                            varDetalii = ((Long) detalii.getProperty(var)).doubleValue();
                        }
                        if ((req.getParameter(var) == null) || req.getParameter(var).isEmpty()) {
                            if ((varDetalii == null)
                                    || ((req.getParameter(var) != null) && req.getParameter(var).isEmpty())) {
                                return null;// vars.put(var, 0d);
                            } else {
                                vars.put(var, varDetalii);
                            }
                        } else {
                            vars.put(var,
                                    aproxDouble(Double.parseDouble(req.getParameter(var).replace(",", ".")), az, nz));
                        }
                    }
                }
            }
            for (Entry<String, Double> var : vars.entrySet()) {
                formula = formula.replaceAll(var.getKey(), var.getValue().toString());
            }
            while (formula.contains("(")) {
                String miniEcuation = formula.substring(formula.lastIndexOf("("),
                        formula.indexOf(")", formula.lastIndexOf("(")) + 1);
                Double cse = calculateSimpleExpresion(miniEcuation.replace("(", "").replace(")", ""), az, nz);
                cse = aproxDouble(cse, az, nz);
                formula = formula.replace(miniEcuation, cse.toString());
            }
            return aproxDouble(calculateSimpleExpresion(formula, az, nz), az, nz);
        } catch (NumberFormatException nfe) {
            Logger log = Logger.getLogger("CalculateFormula");
            log.log(Level.WARNING, "Stack Trace", nfe);
            throw new InvalidField("Unul din numere nu este in formatul corect");
        }
    }

    public static Double aproxDouble(Double nr, String type, String nrZecimale) {
        if ((type == null) || type.isEmpty()) {
            return nr;
        }
        BigDecimal nrZZ = null;
        BigDecimal nrBigDecimal = BigDecimal.valueOf(nr);

        try {
            nrZZ = new BigDecimal(nrZecimale);
        } catch (NumberFormatException ignored) {
        }
        if (nrZZ != null) {
            if (type.equals("celMaiApropiatNumar")) {

                return nrBigDecimal.round(new MathContext(nrZZ.intValue() + 1, RoundingMode.HALF_UP)).doubleValue();
            }
            if (type.equals("prinAdaugare")) {
                return nrBigDecimal.round(new MathContext(nrZZ.intValue() + 1, RoundingMode.CEILING)).doubleValue();
            }
            if (type.equals("prinScadere")) {
                return nrBigDecimal.round(new MathContext(nrZZ.intValue() + 1, RoundingMode.FLOOR)).doubleValue();
            }
        }
        if (type.equals("faraAproximare")) {
            return nr;
        }
        return nr;
    }

    public static Double calculateSimpleExpresion(String formula, String aproxZecimale, String numarZecimale) throws NumberFormatException,
            InvalidField {
        if (formula.matches("^[+-]?[0-9]*[.]?[0-9]*$")) {
            return aproxDouble(Double.parseDouble(formula), aproxZecimale, numarZecimale);
        }
        Matcher m = Pattern.compile("([+-]?\\d*\\.?\\d*)([+-/*]?)").matcher(formula);
        ArrayList<String> groups = new ArrayList<>();
        while (m.find()) {
            if (!m.group(1).isEmpty()) {
                groups.add(m.group(1));
            }
            if (!m.group(2).isEmpty()) {
                groups.add(m.group(2));
            }
        }
        Double firstValue;
        Double secondValue;
        String operator;
        if ((groups.size() % 2) == 1) {
            for (int i = 0; i < (groups.size() - 1); ) {
                operator = groups.get(i + 1);
                if (operator.equals("*") || operator.equals("/")) {
                    secondValue = aproxDouble(Double.parseDouble(groups.get(i + 2)), aproxZecimale, numarZecimale);
                    firstValue = aproxDouble(Double.parseDouble(groups.get(i)), aproxZecimale, numarZecimale);
                    groups.remove(i);
                    groups.remove(i);
                    groups.remove(i);// scoatem 2 valori si un operator din lista
                    groups.add(i, calc(firstValue, secondValue, operator).toString());
                } else {
                    i += 2;
                }
            }
            for (int i = 0; i < (groups.size() - 1); ) {
                operator = groups.get(i + 1);
                secondValue = aproxDouble(Double.parseDouble(groups.get(i + 2)), aproxZecimale, numarZecimale);
                firstValue = aproxDouble(Double.parseDouble(groups.get(i)), aproxZecimale, numarZecimale);
                groups.remove(i);
                groups.remove(i);
                groups.remove(i); // scoatem 2 valori si un operator din lista
                groups.add(i, calc(firstValue, secondValue, operator).toString());
            }
            firstValue = aproxDouble(Double.parseDouble(groups.get(0)), aproxZecimale, numarZecimale);
        } else {
            Logger log = Logger.getLogger("CalculateFormula");
            log.log(Level.WARNING, GsonUtils.getGson().toJson(groups));
            throw new InvalidField("Aparut o eroare la parsarea formulei de calcul a campului calculat");
        }
        return firstValue;

    }

    public static Double calc(Double first, Double second, String operator) throws InvalidField {
        BigDecimal f = BigDecimal.valueOf(first);
        BigDecimal s = BigDecimal.valueOf(second);

        operator = operator.trim();
        if (operator.equals("+")) {
            return f.add(s).doubleValue();
        }
        if (operator.equals("*")) {
            return f.multiply(s).doubleValue();
        }
        if (operator.equals("-")) {
            return f.subtract(s).doubleValue();
        }

        if (operator.equals("/")) {
            if (s.equals(BigDecimal.ZERO)) {
                return null;
            } else {
                return f.divide(s).doubleValue();
            }
        }
        throw new InvalidField("Operatorul: " + operator + " nu este in formatul corect ! ");
    }

}
