package ro.google.appenigne;

import com.google.appengine.api.datastore.Entity;

public class DatastoreCallbacks  extends AbstractDatastoreCallbacks{
    public void prePut(Entity entity){
        if ("DetaliiStudent".equals(entity.getKind())) {

            //Callbacks.removeFeedBack(entity);
        }
    }
    public void postPut(Entity entity){
        switch (entity.getKind()) {
            case "IstoricClient":
            case "IstoricSemestru":
            case "IstoricAnStudiu":
            case "IstoricAnUniversitar":
            case "IstoricSpecializare":
            case "IstoricDisciplina":
            case "IstoricStudent":
            case "IstoricCandidat":
            case "IstoricCont":
            case "IstoricNota":
            case "Nota":
                //Callbacks.triggerFormulas(entity);
                break;
        }
        if ("IstoricStudent".equals(entity.getKind())) {
            //Callbacks.updateConturi(entity);
        }
        if ("Cont".equals(entity.getKind())) {
            //Callbacks.updateCache(entity);
        }
        if ("Banner".equals(entity.getKind())) {
            //Callbacks.updateCacheBanners(entity);
        }
        if ("Language".equals(entity.getKind())) {
            //Callbacks.updateCacheLanguage(entity);
        }
    }

}
