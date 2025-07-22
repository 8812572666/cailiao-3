package com.material.management.model;

/**
 * 数据库信息模型
 * 
 * @author Material Management System
 * @version 1.0.0
 */
public class DatabaseInfo {
    private String name;
    private int tableCount;
    private long totalRows;

    public DatabaseInfo() {}

    public DatabaseInfo(String name) {
        this.name = name;
    }

    public DatabaseInfo(String name, int tableCount, long totalRows) {
        this.name = name;
        this.tableCount = tableCount;
        this.totalRows = totalRows;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTableCount() {
        return tableCount;
    }

    public void setTableCount(int tableCount) {
        this.tableCount = tableCount;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    @Override
    public String toString() {
        return "DatabaseInfo{" +
                "name='" + name + '\'' +
                ", tableCount=" + tableCount +
                ", totalRows=" + totalRows +
                '}';
    }
}
