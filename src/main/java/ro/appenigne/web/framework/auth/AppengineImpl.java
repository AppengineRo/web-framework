package ro.appenigne.web.framework.auth;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.brickred.socialauth.*;
import org.brickred.socialauth.exception.AccessTokenExpireException;
import org.brickred.socialauth.exception.SocialAuthException;
import org.brickred.socialauth.oauthstrategy.OAuthStrategyBase;
import org.brickred.socialauth.util.AccessGrant;
import org.brickred.socialauth.util.Response;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Provider implementation for Yahoo. This uses the oAuth API provided by Yahoo
 *
 * @author abhinavm@brickred.com
 * @author tarunn@brickred.com
 */
public class AppengineImpl extends AbstractProvider implements AuthProvider, Serializable {


    public AppengineImpl() throws Exception {
    }

    @Override
    protected List<String> getPluginsList() {
        return null;
    }

    @Override
    protected OAuthStrategyBase getOauthStrategy() {
        return null;
    }

    @Override
    public String getLoginRedirectURL(String s) throws Exception {
        UserService userService = UserServiceFactory.getUserService();
        return userService.createLoginURL(s);
    }

    @Override
    public Profile verifyResponse(Map<String, String> map) throws Exception {
        return getUserProfile();
    }

    @Override
    public Response updateStatus(String s) throws Exception {
        return null;
    }

    @Override
    public List<Contact> getContactList() throws Exception {
        return null;
    }

    @Override
    public Profile getUserProfile() throws Exception {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user != null) {
            Profile profile = new Profile();
            profile.setEmail(user.getEmail());
            profile.setProviderId(getProviderId());
            return profile;
        }
        return null;
    }

    @Override
    public void logout() {

    }

    @Override
    public void setPermission(Permission permission) {

    }

    @Override
    public Response api(String s, String s1, Map<String, String> map, Map<String, String> map1, String s2) throws Exception {
        return null;
    }

    @Override
    public AccessGrant getAccessGrant() {
        return null;
    }

    @Override
    public String getProviderId() {
        return "appengine";
    }

    @Override
    public void setAccessGrant(AccessGrant accessGrant) throws AccessTokenExpireException, SocialAuthException {

    }

    @Override
    public Response uploadImage(String s, String s1, InputStream inputStream) throws Exception {
        return null;
    }
}
