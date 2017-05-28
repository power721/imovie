package org.har01d.imovie.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private boolean isLogin;
    private Random random = new Random();

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
            Config dbcl2 = service.getConfig(TOKEN_KEY);
            if (dbcl2 == null) {
                login();
            } else {
                BasicClientCookie cookie = new BasicClientCookie(TOKEN_KEY, dbcl2.getValue());
                cookie.setDomain(".movie.douban.com");
                cookie.setPath("/");
                cookieStore.addCookie(cookie);

                // must set bid before get https://movie.douban.com/
                cookie = new BasicClientCookie("bid", genBid(12));
                cookie.setDomain(".movie.douban.com");
                cookie.setPath("/");
                cookieStore.addCookie(cookie);

                try {
                    String html = HttpUtils.get("https://movie.douban.com/subject/1307528/", null, cookieStore);
                    if (html != null && html.contains("盲井")) {
                        isLogin = true;
                        logger.info("user is logged in DouBan.");
                        return;
                    }
                } catch (IOException e) {
                    logger.warn("check DouBan login failed.", e);
                    service.publishEvent("DB login", "check DouBan login failed: " + e.getMessage());
                }
                login();
            }
        } catch (Exception e) {
            logger.warn("login to DouBan failed.", e);
            service.publishEvent("DB login", "login to DouBan failed: " + e.getMessage());
        }
    }

    @Override
    public void reLogin() {
        Config config = service.getConfig(TOKEN_KEY);
        if (config != null) {
            service.deleteConfig(config);
        }

        tryLogin();
    }

    private void login() throws IOException {
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
        BasicCookieStore cookieStore1 = HttpUtils.post4Cookie("https://accounts.douban.com/login?source=movie", params);
        List<Cookie> cookies = cookieStore1.getCookies();
        for (Cookie cookie : cookies) {
            if (TOKEN_KEY.equals(cookie.getName())) {
                service.saveConfig(TOKEN_KEY, cookie.getValue());
                cookieStore.addCookie(cookie);
                isLogin = true;
                logger.info("Login to DouBan successfully, user: " + user.getValue());
            } else if ("bid".equals(cookie.getName())) {
                cookieStore.addCookie(cookie);
            }
        }
        logger.warn("Login to DouBan failed, user: " + user.getValue());
    }

    @Override
    public void updateCookie() {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if ("bid".equals(cookie.getName())) {
                if (cookie instanceof BasicClientCookie) {
                    String bid = genBid(11);
                    BasicClientCookie basicClientCookie = (BasicClientCookie) cookie;
                    basicClientCookie.setValue(bid);
                    logger.info("[DB] change bid to {}", basicClientCookie.getValue());
                    return;
                }
            }
        }

        BasicClientCookie cookie = new BasicClientCookie("bid", genBid(11));
        cookie.setDomain(".movie.douban.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        logger.info("[DB] generate new bid {}", cookie.getValue());
    }

    private String genBid(int len) {
        char[] bids = new char[len];
        for (int i = 0; i < len; ++i) {
            bids[i] = CHARS[random.nextInt(CHARS.length)];
        }

        return new String(bids);
    }

}
