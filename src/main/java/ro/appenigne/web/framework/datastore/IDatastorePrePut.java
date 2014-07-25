package ro.appenigne.web.framework.datastore;

import com.google.appengine.api.datastore.Entity;

import javax.servlet.http.HttpServletRequest;

public interface IDatastorePrePut {

    /**
     * Override this if you want datastoreCallbacks
     * @param entity the entity before it's prePut
     */
    public void prePut(Entity entity, HttpServletRequest req);

}
