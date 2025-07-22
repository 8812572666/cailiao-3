package com.material.management.model;

import java.util.List;

/**
 * 表信息模型
 * 
 * @author Material Management System
 * @version 1.0.0
 */
public class TableInfo {
    private String name;
    private long rowCount;
    private int columnCount;
    private List<ColumnInfo> columns;

    public TableInfo() {}

    public TableInfo(String name, long rowCount, int columnCount) {
        this.name = name;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "TableInfo{" +
                "name='" + name + '\'' +
                ", rowCount=" + rowCount +
                ", columnCount=" + columnCount +
                ", columns=" + columns +
                '}';
    }
}
