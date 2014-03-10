package ro.appenigne.web.framework.form;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Text;
import ro.appenigne.web.framework.exception.InvalidField;
import ro.appenigne.web.framework.utils.GsonUtils;
import ro.appenigne.web.framework.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class JsonUtils {
    public static Object getObject(HttpServletRequest req, Map<String, List<BlobKey>> blobs, Entity detalii, LinkedHashMap<String, Object> element) throws InvalidField {
        BlobInfoFactory blobInfoFactory = new BlobInfoFactory();
        String elName = (String) element.get("name");
        String elType = (String) element.get("dbType");
        Object val = null;
        if (elType.equals("long")) {
            try {
                Double valDouble = Double.parseDouble(req.getParameter(elName));
                val = valDouble.longValue();
            } catch (NumberFormatException ignored) {
            }
        }
        if (elType.equals("double")) {
            String formula = (String) element.get("formula");
            String nz = element.get("numarZecimale").toString();
            String az = element.get("aproximareZecimale").toString();
            if (formula != null && !formula.isEmpty()) {
                val = FormValidate.calculateField(element, detalii, req);
            } else {
                try {
                    if (req.getParameter(elName) != null) {
                        val = FormValidate.aproxDouble(Double.parseDouble(req.getParameter(elName)
                                .replace(",", ".")), az, nz);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (val != null) {
                if (Double.isInfinite((Double) val) || Double.isNaN((Double) val)) {
                    val = null;
                }
            }
        }
        if (elType.equals("boolean")) {
            val = req.getParameter(elName) != null && !req.getParameter(elName).isEmpty();
        }
        if (elType.equals("string")) {
            val = (req.getParameter(elName) == null) ? "" : req.getParameter(elName).trim();
            String aClass = (String) element.get("class");
            if (aClass != null) {
                if (aClass.contains("valueTransformSentenceCase")) {
                    val = StringUtils.sentenceCase((String) val);
                }
                if (aClass.contains("valueTransformCapitalize")) {
                    val = StringUtils.capitalizeWords((String) val);
                }
                if (aClass.contains("valueTransformUppercase")) {
                    val = ((String) val).toUpperCase();
                }
                if (aClass.contains("valueTransformLowercase")) {
                    val = ((String) val).toLowerCase();
                }
            }
        }
        if (elType.equals("text")) {
            val = new Text(req.getParameter(elName));
        }
        if (elType.equals("date")) {
            try {
                val = new Date(java.lang.Long.parseLong(req.getParameter(elName)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (elType.equals("list")) {
            List<String> curentList = new ArrayList<>();
            if (req.getParameter("_addProperty") != null &&
                    (req.getParameter("_addProperty").equals(elName) || req.getParameter("_addProperty").equals(elName.replace("[]", "")))) {
                Object currentValue = detalii.getProperty(elName.replace("[]", ""));

                if (currentValue != null) {
                    if (currentValue instanceof String) {
                        curentList.add((String) currentValue);
                    } else if (currentValue instanceof List<?>) {
                        curentList.addAll((List<String>) currentValue);
                    }
                }
            }
            try {
                ArrayList<String> multiVal = new ArrayList<>(Arrays.asList(req.getParameterValues(elName)));
                ArrayList<String> empty = new ArrayList<>();
                empty.add("");
                empty.add(" ");
                multiVal.removeAll(empty);
                if (!curentList.isEmpty()) {
                    HashSet<String> set = new HashSet<>();
                    set.addAll(curentList);
                    set.addAll(multiVal);
                    multiVal = new ArrayList<>();
                    multiVal.addAll(set);
                    val = multiVal;
                } else {
                    val = multiVal;
                }
            } catch (NullPointerException e) {
                val = new ArrayList();
            }
        }
        if (elType.equals("listBlob")) {

            List<BlobKey> blobKey = blobs.get(elName);
            if (blobKey == null) {
                blobKey = new ArrayList<>();
            }
            if (req.getParameterValues(elName) != null) {
                for (String blobStr : req.getParameterValues(elName)) {
                    if (blobStr == null || blobStr.isEmpty()) {
                        continue;
                    }
                    blobKey.add(new BlobKey(blobStr));
                }
            }
            if (blobKey.size() == 0) {
                val = null;
            } else {
                List<File> files = new ArrayList<>();
                for (BlobKey blob : blobKey) {
                    File file = new File();
                    BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blob);
                    file.name = blobInfo.getFilename();
                    file.blobKey = blob.getKeyString();
                    file.size = blobInfo.getSize();
                    file.type = blobInfo.getContentType();
                    files.add(file);
                }
                val = new Text(GsonUtils.getGson().toJson(files));
            }
        }
        if (elType.equals("blob")) {
            List<BlobKey> blobKey = blobs.get(elName);
            if (blobKey == null) {
                blobKey = new ArrayList<>();
            }
            if (req.getParameterValues(elName) != null) {
                for (String blobStr : req.getParameterValues(elName)) {
                    if (blobStr == null || blobStr.isEmpty()) {
                        continue;
                    }
                    blobKey.add(new BlobKey(blobStr));
                }
            }
            if (blobKey.size() == 0) {
                val = null;
            } else {
                File file = new File();
                BlobKey blob = blobKey.get(0); //save the first blob only, preferred the uploaded one
                BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blob);
                file.name = blobInfo.getFilename();
                file.blobKey = blob.getKeyString();
                file.size = blobInfo.getSize();
                file.type = blobInfo.getContentType();
                val = new Text(GsonUtils.getGson().toJson(file));
            }
        }
        if (elType.equals("geoPt")) {
            ArrayList<String> geoVals = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(req.getParameter(elName), ",");
            while (st.hasMoreElements()) {
                geoVals.add(st.nextToken());
            }

            Float latVal = Float.parseFloat(geoVals.get(0));
            Float longVal = Float.parseFloat(geoVals.get(1));
            try {
                val = new GeoPt(latVal, longVal);
            } catch (IllegalArgumentException e) {
                throw new InvalidField("Nu s-a putut face o instanta GeoPt!");
            }
        }
        if (elType.equals("geoPtList")) {
            ArrayList<GeoPt> geoPointList = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(req.getParameter(elName), ";");
            while (st.hasMoreElements()) {
                String geoVals = st.nextToken();
                StringTokenizer tok = new StringTokenizer(geoVals, ",");
                Float latVal = Float.parseFloat(tok.nextToken());
                Float longVal = Float.parseFloat(tok.nextToken());
                try {
                    GeoPt geoPoint = new GeoPt(latVal, longVal);
                    geoPointList.add(geoPoint);
                    val = geoPointList;
                } catch (IllegalArgumentException e) {
                    throw new InvalidField("Nu s-a putut face o instanta [GeoPt]!");
                }
            }
        }
        return val;
    }

    public static class File {
        public String name;
        public String type;
        public long size;
        public String blobKey;

        public File() {
        }
    }

}
