package com.hpcloud.mon.Config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfluxDBConfig {


    @JsonProperty
    String name;

    public String getName() {
        return name;
    }

    @JsonProperty
    int replicationFactor;

    public int getReplicationFactor() {
        return replicationFactor;
    }

    @JsonProperty
    String url;

    public String getUrl() {
        return url;
    }

    @JsonProperty
    String user;

    public String getUser() {
        return user;
    }

    @JsonProperty
    String password;

    public String getPassword() {
        return password;
    }


}
