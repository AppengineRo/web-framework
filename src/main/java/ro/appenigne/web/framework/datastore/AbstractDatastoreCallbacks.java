package ro.appenigne.web.framework.datastore;

import com.google.appengine.api.datastore.Entity;

public class AbstractDatastoreCallbacks {

    /**
     * Override this if you want datastoreCallbacks
     * @param entity the entity before it's prePut
     */
    public void prePut(Entity entity){

    }

    /**
     * Override this if you want datastoreCallbacks
     * @param entity the entity after is put
     */
    public void postPut(Entity entity){

    }
}
