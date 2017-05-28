package org.har01d.imovie.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DouBanServiceImpl implements DouBanService {

    private static final String TOKEN_KEY = "dbcl2";
    private static final Logger logger = LoggerFactory.getLogger(DouBanService.class);

    private boolean isLogin;

    @Autowired
    private MovieService service;

    @Autowired
    private BasicCookieStore cookieStore;

    @Override
    public boolean isLogin() {
        return isLogin;
    }

    @Override
    public void tryLogin() {
        try {
            Config config = service.getConfig(TOKEN_KEY);
            if (config == null) {
                login();
            } else {
                BasicClientCookie cookie = new BasicClientCookie(TOKEN_KEY, config.getValue());
                cookie.setDomain(".movie.douban.com");
                cookie.setPath("/");
                cookieStore.addCookie(cookie);

                try {
                    String html = HttpUtils.get("https://movie.douban.com/subject/1307528/", null, cookieStore);
                    if (html.contains("盲井")) {
                        isLogin = true;
                        logger.info("DouBan is logged in.");
                        return;
                    }
                } catch (IOException e) {
                    logger.warn("get DouBan account failed.", e);
                }
                login();
            }
        } catch (Exception e) {
            logger.warn("login to DouBan failed.", e);
        }
    }

    private Cookie login() throws IOException {
        Config user = service.getConfig("db_user");
        if (user == null) {
            throw new IOException("Cannot get db_user from table config.");
        }

        Config password = service.getConfig("db_password");
        if (password == null) {
            throw new IOException("Cannot get db_password from table config.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("source", "movie");
        params.put("form_email", user.getValue());
        params.put("form_password", password.getValue());
        params.put("remember", "on");
        params.put("login", "登录");
        BasicCookieStore cookieStore = HttpUtils.post4Cookie("https://accounts.douban.com/login?source=movie", params);
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if (TOKEN_KEY.equals(cookie.getName())) {
                service.saveConfig(TOKEN_KEY, cookie.getValue());
                cookieStore.addCookie(cookie);
                isLogin = true;
                logger.info("Login to DouBan successfully, user: " + user.getValue());
                return cookie;
            }
        }
        logger.warn("Login to DouBan failed, user: " + user.getValue());
        return null;
    }

}
