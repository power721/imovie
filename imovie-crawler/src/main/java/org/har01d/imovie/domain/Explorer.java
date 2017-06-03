package org.har01d.imovie.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Explorer {

    @Id
    private String uri;

    private String type;

    public Explorer() {
    }

    public Explorer(String type, String uri) {
        this.type = type;
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
