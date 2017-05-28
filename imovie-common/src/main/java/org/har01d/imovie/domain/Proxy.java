package org.har01d.imovie.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.apache.http.HttpHost;

@Entity
public class Proxy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hostname;

    private int port;

    private String scheme = "HTTP";

    public Proxy() {
    }

    public Proxy(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public Proxy(String hostname, int port, String scheme) {
        this.hostname = hostname;
        this.port = port;
        this.scheme = scheme;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public HttpHost toHttpHost() {
        return new HttpHost(hostname, port, scheme);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Proxy proxy = (Proxy) o;

        if (port != proxy.port) {
            return false;
        }
        if (!hostname.equals(proxy.hostname)) {
            return false;
        }
        return scheme.equals(proxy.scheme);
    }

    @Override
    public int hashCode() {
        int result = hostname.hashCode();
        result = 31 * result + port;
        result = 31 * result + scheme.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return scheme + "://" + hostname + ":" + port;
    }

}
