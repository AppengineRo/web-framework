package ro.appenigne.web.framework.auth;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;
import ro.appenigne.web.framework.utils.Log;
import ro.appenigne.web.framework.utils.RequestUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AuthService {
    private AppEngineSession session;
    private SocialAuthManager authManager = null;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public AuthService(HttpServletRequest req, HttpServletResponse resp) {
        this.request = req;
        this.response = resp;
        this.session = new AppEngineSession(req);
        if (this.session.getSession() == null && this.session.getGAESESS() != null) {
            Cookie cookie = new Cookie("GAESESS", this.session.getGAESESS());
            cookie.setMaxAge(0);
            cookie.setPath("/");
            Log.d("sterge cookie",this.session.getGAESESS());
            resp.addCookie(cookie);
        }
        if (session.getAttribute("authManager") != null) {
            authManager = (SocialAuthManager) session.getAttribute("authManager");
        }
    }

    public boolean isUserAdmin() {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user != null) {
            return userService.isUserAdmin();
        }
        return false;
    }

    public Profile getCurrentUser() {
        return getCurrentProfile();
    }

    public Profile getCurrentProfile() {
        if (authManager != null && authManager.getCurrentAuthProvider() != null) {
            try {
                AuthProvider currentAuthProvider = authManager.getCurrentAuthProvider();
                if(currentAuthProvider != null){
                    return currentAuthProvider.getUserProfile();
                }
            } catch (Exception e) {
                Log.w(e);
            }
        }
        return null;
    }

    public String getAuthDomain() {
        Profile profile = getCurrentProfile();
        if (profile != null) {
            return profile.getProviderId();
        }
        return null;
    }

    public String createLoginURL(String provider, String returnUrl) {
        try {
            SocialAuthConfig socialAuthConfig = SocialAuthConfig.getDefault();
            socialAuthConfig.addProvider("appengine", AppengineImpl.class);
            socialAuthConfig.addProvider("yahoo", YahooImpl.class);
            socialAuthConfig.load(new FileInputStream(System.getProperty("auth-config", "WEB-INF/auth-config/oauth_consumer.properties")));
            authManager = new SocialAuthManager();
            authManager.setSocialAuthConfig(socialAuthConfig);
            String loginUrl = authManager.getAuthenticationUrl(provider, getConnectUrl(returnUrl));
            session.setAttribute("authManager", authManager, response);
            return loginUrl;
        } catch (Exception e) {
            Log.w(provider, e);
        }
        return null;
    }

    public String createLogoutURL(String returnUrl) throws UnsupportedEncodingException {
        return  RequestUtils.getNaketUrl(request)+"/disconnect?redirect_to=" + URLEncoder.encode(returnUrl, "UTF-8");
    }

    private String getConnectUrl (String returnUrl) throws UnsupportedEncodingException {
        return  RequestUtils.getNaketUrl(request)+"/connect?redirect_to=" + URLEncoder.encode(returnUrl, "UTF-8");
    }
}
