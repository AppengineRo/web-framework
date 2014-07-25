package ro.appenigne.web.framework.datastore;

import com.google.appengine.api.datastore.Entity;

import javax.servlet.http.HttpServletRequest;

public class KeysOnlyDatastoreCallback implements IDatastorePrePut{
    @Override
    public void prePut(Entity entity, HttpServletRequest req) {
        if(entity.getProperties().size() == 0){
            throw new RuntimeException("Entity has no properties");
        }
    }
}
