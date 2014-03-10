package ro.appenigne.web.framework.utils;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;

public class BrowserUtils {

    public static Browser getBrowser(HttpServletRequest req) {
        if (req.getHeader("User-Agent") == null) {
            return Browser.UNKNOWN;
        }
        UserAgent ua = UserAgent.parseUserAgentString(req.getHeader("User-Agent"));
        return ua.getBrowser();
    }
}
