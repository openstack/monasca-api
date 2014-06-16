package com.hpcloud.mon.Config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseConfiguration {

    @JsonProperty
    String databaseType;

    public String getDatabaseType() { return databaseType; }
}
