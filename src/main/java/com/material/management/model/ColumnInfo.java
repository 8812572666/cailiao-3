package com.material.management.model;

/**
 * 列信息模型
 * 
 * @author Material Management System
 * @version 1.0.0
 */
public class ColumnInfo {
    private String name;
    private String type;
    private boolean nullable;
    private String key;
    private String defaultValue;
    private String extra;

    public ColumnInfo() {}

    public ColumnInfo(String name, String type, boolean nullable, String key, String defaultValue, String extra) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.key = key;
        this.defaultValue = defaultValue;
        this.extra = extra;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", nullable=" + nullable +
                ", key='" + key + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", extra='" + extra + '\'' +
                '}';
    }
}
