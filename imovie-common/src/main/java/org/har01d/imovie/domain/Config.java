package org.har01d.imovie.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Config {

    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    public Config() {
    }

    public Config(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" + key + "=" + value + "}";
    }

}
