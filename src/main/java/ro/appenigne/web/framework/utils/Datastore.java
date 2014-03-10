package ro.appenigne.web.framework.utils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import ro.appenigne.web.framework.datastore.AbstractDatastoreCallbacks;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class Datastore implements DatastoreService {
    public DatastoreService datastore;
    public AsyncDatastoreService asyncDatastoreService;
    MemcacheService memcacheService;
    private ArrayList<Future<?>> toPutObjects = new ArrayList<>();
    private ArrayList<Entity> postPutEntities = new ArrayList<>();
    private Map<Key, Entity> cacheEntities = new HashMap<>();
    AbstractDatastoreCallbacks datastoreCallbacks = null;

    public Datastore() {
        datastore = DatastoreServiceFactory.getDatastoreService();
        asyncDatastoreService = DatastoreServiceFactory.getAsyncDatastoreService();
        memcacheService = MemcacheServiceFactory.getMemcacheService();
        memcacheService.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.SEVERE));

        String datastoreCallbacksClass = System.getProperty("datastoreCallbacksClass");
        if (datastoreCallbacksClass != null && !datastoreCallbacksClass.isEmpty()) {
            try {
                Class<?> controllerClass = Class.forName(datastoreCallbacksClass);
                if (AbstractDatastoreCallbacks.class.isAssignableFrom(controllerClass)) {
                    datastoreCallbacks = (AbstractDatastoreCallbacks) controllerClass.newInstance();
                }
            } catch (Exception e) {
                Log.s(e);
            }
        }
    }

    public Entity getFromMemOrDb(String _hashContCurent) throws EntityNotFoundException {
        if (_hashContCurent == null || _hashContCurent.isEmpty()) {
            return null;
        }
        Key keyCont = KeyFactory.stringToKey(_hashContCurent);
        Entity cont = (Entity) memcacheService.get(_hashContCurent); // read from cache
        if (cont == null) {
            cont = this.get(keyCont);
            memcacheService.put(_hashContCurent, cont, Expiration.byDeltaSeconds(600));//10 min
        }// else a luat din memcache --> am eficientizat cu vreo 15ms requestul
        return cont;
    }

    private void triggerPrePut(Entity entity) {
        if(datastoreCallbacks !=null){
            datastoreCallbacks.prePut(entity);
        }
    }

    private void triggerPostPut(Entity entity) {
        if(datastoreCallbacks !=null){
            datastoreCallbacks.postPut(entity);
        }
    }

    public Entity cacheGet(Key k) throws EntityNotFoundException {
        Entity cached = cacheEntities.get(k);
        if (cached != null) {
            return cached;
        } else {
            Entity fromDb = datastore.get(k);
            cacheEntities.put(k, fromDb);
            return fromDb;
        }
    }

    public Map<Key, Entity> cacheGet(Iterable<Key> keys) {
        Map<Key, Entity> results = new HashMap<>();
        List<Key> notFound = new ArrayList<>();
        for (Key k : keys) {
            Entity c = cacheEntities.get(k);
            if (c != null) {
                results.put(k, c);
            } else {
                notFound.add(k);
            }
        }
        if (!notFound.isEmpty()) {
            Map<Key, Entity> fromDb = datastore.get(notFound);
            cacheEntities.putAll(fromDb);
            results.putAll(fromDb);
        }
        return results;
    }

    public void toPut(Entity entity) {
        triggerPrePut(entity);
        toPutObjects.add(asyncDatastoreService.put(entity));
        postPutEntities.add(entity);
    }

    public void toPut(Entity... entities) {
        for (Entity entity : entities) {
            triggerPrePut(entity);
            toPutObjects.add(asyncDatastoreService.put(entity));
            postPutEntities.add(entity);
        }
    }

    public void toPut(Iterable<Entity> entities) {
        for (Entity entity : entities) {
            triggerPrePut(entity);
            postPutEntities.add(entity);
        }
        toPutObjects.add(asyncDatastoreService.put(entities));

    }

    public void commit() throws ExecutionException, InterruptedException {
        for (Future<?> f : toPutObjects) {
            f.get();
        }
        for (Entity entity : postPutEntities) {
            triggerPostPut(entity);
        }
    }

    @Override
    public Key put(Entity entity) {
        triggerPrePut(entity);
        Key result = datastore.put(entity);
        triggerPostPut(entity);
        return result;
    }

    @Override
    public Key put(Transaction transaction, Entity entity) {
        triggerPrePut(entity);
        Key result = datastore.put(transaction, entity);
        triggerPostPut(entity);
        return result;
    }

    @Override
    public List<Key> put(Iterable<Entity> entities) {
        for (Entity e : entities) {
            triggerPrePut(e);
        }
        List<Key> result = datastore.put(entities);
        for (Entity e : entities) {
            triggerPostPut(e);
        }
        return result;
    }

    @Override
    public List<Key> put(Transaction transaction, Iterable<Entity> entities) {
        for (Entity e : entities) {
            triggerPrePut(e);
        }
        List<Key> result = datastore.put(transaction, entities);
        for (Entity e : entities) {
            triggerPostPut(e);
        }
        return result;
    }

    //all items below call the functions in datastore;
    @Override
    public void delete(Key... keys) {
        datastore.delete(keys);
    }

    @Override
    public void delete(Transaction transaction, Key... keys) {
        datastore.delete(transaction, keys);
    }

    @Override
    public void delete(Iterable<Key> keys) {
        datastore.delete(keys);
    }

    @Override
    public void delete(Transaction transaction, Iterable<Key> keys) {
        datastore.delete(transaction, keys);
    }

    @Override
    public Transaction beginTransaction() {
        return datastore.beginTransaction();
    }

    @Override
    public Transaction beginTransaction(TransactionOptions transactionOptions) {
        return datastore.beginTransaction(transactionOptions);
    }

    @Override
    public KeyRange allocateIds(String s, long l) {
        return datastore.allocateIds(s, l);
    }

    @Override
    public KeyRange allocateIds(Key key, String s, long l) {
        return datastore.allocateIds(key, s, l);
    }

    @Override
    public KeyRangeState allocateIdRange(KeyRange keys) {
        return datastore.allocateIdRange(keys);
    }

    @Override
    public DatastoreAttributes getDatastoreAttributes() {
        return datastore.getDatastoreAttributes();
    }

    @Override
    public Map<Index, Index.IndexState> getIndexes() {
        return datastore.getIndexes();
    }

    @Override
    public Entity get(Key k) throws EntityNotFoundException {
        return datastore.get(k);
    }

    @Override
    public Entity get(Transaction transaction, Key key) throws EntityNotFoundException {
        return datastore.get(transaction, key);
    }

    @Override
    public Map<Key, Entity> get(Iterable<Key> keys) {
        return datastore.get(keys);
    }

    @Override
    public Map<Key, Entity> get(Transaction transaction, Iterable<Key> keys) {
        return datastore.get(transaction, keys);
    }

    @Override
    public PreparedQuery prepare(Query q) {
        return datastore.prepare(q);
    }

    @Override
    public PreparedQuery prepare(Transaction transaction, Query query) {
        return datastore.prepare(transaction, query);
    }

    @Override
    public Transaction getCurrentTransaction() {
        return datastore.getCurrentTransaction();
    }

    @Override
    public Transaction getCurrentTransaction(Transaction transaction) {
        return datastore.getCurrentTransaction(transaction);
    }

    @Override
    public Collection<Transaction> getActiveTransactions() {
        return datastore.getActiveTransactions();
    }
}
