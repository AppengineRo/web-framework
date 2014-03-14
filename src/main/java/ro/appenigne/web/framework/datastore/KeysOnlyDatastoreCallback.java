package ro.appenigne.web.framework.datastore;

import com.google.appengine.api.datastore.Entity;

public class KeysOnlyDatastoreCallback implements IDatastorePrePut{
    @Override
    public void prePut(Entity entity) {
        if(entity.getProperties().size() == 0){
            throw new RuntimeException("Entity has no properties");
        }
    }
}
