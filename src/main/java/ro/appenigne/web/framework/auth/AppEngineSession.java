package ro.appenigne.web.framework.auth;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import ro.appenigne.web.framework.utils.Log;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.logging.Level;

/**
 * Created by cosmin on 20/04/15.
 */
public class AppEngineSession {
    public int MAX_AGE = 24 * 60 * 60; // 24 hours.
    private Entity session = null;
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    public AppEngineSession(HttpServletRequest req) {
        memcache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.WARNING));
        Cookie[] cookies = req.getCookies();
        if(cookies !=null) {
            for (Cookie cookie : cookies) {
                if ("GAESESS".equals(cookie.getName())) {
                    String hash = cookie.getValue();
                    session = (Entity) memcache.get(hash);
                    if (session == null) {
                        try {
                            NamespaceManager.set("");
                            session = datastore.get(KeyFactory.stringToKey(hash));
                            memcache.put(hash, session, Expiration.onDate((Date) session.getProperty("expiration")));
                        } catch (Exception e) {
                            Log.w(e);
                        }
                    }
                    if (session != null) {
                        Date expiration = (Date) session.getProperty("expiration");
                        if (expiration.before(new Date())) {
                            memcache.delete(KeyFactory.keyToString(session.getKey()));
                            datastore.delete(session.getKey());
                            session = null;
                        }
                    }
                    break;
                }
            }
        }

    }

    public void setAttribute(String key, Object value, HttpServletResponse resp) {
        NamespaceManager.set("");
        if (session == null) {
            session = new Entity("GAESESS");
            session.setProperty("expiration", new Date((new Date()).getTime() + (MAX_AGE * 1000)));
            datastore.put(session);
            String hash = KeyFactory.keyToString(session.getKey());
            memcache.put(hash, session, Expiration.byDeltaSeconds(MAX_AGE));
            Cookie cookie = new Cookie("GAESESS", hash);
            cookie.setMaxAge(MAX_AGE);
            cookie.setPath("/");
            resp.addCookie(cookie);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(value);
            byte[] bites = bos.toByteArray();
            String hash = KeyFactory.keyToString(session.getKey());
            session.setProperty(key, new Blob(bites));
            memcache.put(hash, session, Expiration.onDate((Date) session.getProperty("expiration")));
            datastore.put(session);
        } catch (IOException e) {
            // ignore close exception
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    public Object getAttribute(String key) {
        if(session!=null) {
            Blob blob = (Blob) session.getProperty(key);
            if (blob != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(blob.getBytes());
                ObjectInput in = null;
                try {
                    in = new ObjectInputStream(bis);
                    return in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    // ignore close exception
                } finally {
                    try {
                        bis.close();
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                }
            }
        }
        return null;
    }


    public void invalidate(HttpServletResponse resp) {
        if (session!=null) {
            String hash = KeyFactory.keyToString(session.getKey());
            memcache.delete(hash);
            datastore.delete(session.getKey());
            Cookie cookie = new Cookie("GAESESS", hash);
            cookie.setMaxAge(MAX_AGE);
            resp.addCookie(cookie);
        }
    }
}
