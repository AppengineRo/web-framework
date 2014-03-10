package ro.appenigne.web.framework.datastore;

import com.google.appengine.api.datastore.Entity;

public interface IDatastorePostPut {

    /**
     * Override this if you want datastoreCallbacks
     * @param entity the entity after is put
     */
    public void postPut(Entity entity);
}
