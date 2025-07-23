package com.material.management.service;

import com.material.management.config.DatabaseConfig;
import com.material.management.model.ColumnInfo;
import com.material.management.model.DatabaseInfo;
import com.material.management.model.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库服务类
 * 提供数据库操作的核心功能
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseConfig databaseConfig;

    @Autowired
    private OssService ossService;

    // 系统数据库列表，需要过滤掉
    private static final Set<String> SYSTEM_DATABASES = Set.of(
        "information_schema", "mysql", "performance_schema", "sys"
    );

    /**
     * 测试数据库连接
     */
    public boolean testConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            logger.info("数据库连接测试成功");
            return true;
        } catch (Exception e) {
            logger.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有用户数据库列表
     */
    public List<DatabaseInfo> getDatabases() {
        try {
            String sql = "SHOW DATABASES";
            List<String> allDatabases = jdbcTemplate.queryForList(sql, String.class);
            
            List<DatabaseInfo> userDatabases = new ArrayList<>();
            for (String dbName : allDatabases) {
                if (!SYSTEM_DATABASES.contains(dbName)) {
                    DatabaseInfo dbInfo = new DatabaseInfo(dbName);
                    userDatabases.add(dbInfo);
                }
            }
            
            logger.info("成功获取 {} 个用户数据库", userDatabases.size());
            return userDatabases;
        } catch (Exception e) {
            logger.error("获取数据库列表失败: {}", e.getMessage());
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage());
        }
    }

    // 表信息缓存
    private final ConcurrentHashMap<String, List<TableInfo>> tableInfoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tableInfoCacheTime = new ConcurrentHashMap<>();
    private static final long TABLE_INFO_CACHE_DURATION = 10 * 60 * 1000; // 10分钟缓存

    /**
     * 获取指定数据库的表信息（使用缓存优化）
     */
    public List<TableInfo> getTables(String databaseName) {
        String cacheKey = databaseName + ".tables";
        Long cacheTime = tableInfoCacheTime.get(cacheKey);

        // 检查缓存是否有效
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < TABLE_INFO_CACHE_DURATION) {
            List<TableInfo> cachedTables = tableInfoCache.get(cacheKey);
            if (cachedTables != null) {
                logger.debug("从缓存获取表信息: {}, 表数: {}", databaseName, cachedTables.size());
                return cachedTables;
            }
        }

        try {
            // 创建连接到指定数据库的JdbcTemplate
            DataSource dataSource = databaseConfig.createDataSourceForDatabase(databaseName);
            JdbcTemplate dbJdbcTemplate = new JdbcTemplate(dataSource);

            // 获取表列表
            String sql = "SHOW TABLES";
            List<String> tableNames = dbJdbcTemplate.queryForList(sql, String.class);

            List<TableInfo> tables = new ArrayList<>();
            for (String tableName : tableNames) {
                TableInfo tableInfo = getTableInfo(dbJdbcTemplate, databaseName, tableName);
                tables.add(tableInfo);
            }

            // 缓存结果
            tableInfoCache.put(cacheKey, tables);
            tableInfoCacheTime.put(cacheKey, System.currentTimeMillis());

            logger.info("成功获取数据库 {} 的 {} 个表信息", databaseName, tables.size());
            return tables;
        } catch (Exception e) {
            logger.error("获取数据库 {} 的表信息失败: {}", databaseName, e.getMessage());
            throw new RuntimeException("获取表信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个表的详细信息（优化版本）
     */
    private TableInfo getTableInfo(JdbcTemplate dbJdbcTemplate, String databaseName, String tableName) {
        try {
            // 使用缓存获取表行数
            Long rowCount = (long) getCachedTableCount(dbJdbcTemplate, databaseName, tableName);

            // 获取表结构（使用缓存）
            List<String> columnNames = getCachedTableStructure(dbJdbcTemplate, databaseName, tableName);

            // 获取详细列信息（只在需要时查询）
            String describeSql = "DESCRIBE `" + tableName + "`";
            List<ColumnInfo> columns = dbJdbcTemplate.query(describeSql, (rs, rowNum) -> {
                ColumnInfo column = new ColumnInfo();
                column.setName(rs.getString("Field"));
                column.setType(rs.getString("Type"));
                column.setNullable("YES".equals(rs.getString("Null")));
                column.setKey(rs.getString("Key"));
                column.setDefaultValue(rs.getString("Default"));
                column.setExtra(rs.getString("Extra"));
                return column;
            });

            TableInfo tableInfo = new TableInfo();
            tableInfo.setName(tableName);
            tableInfo.setRowCount(rowCount != null ? rowCount : 0);
            tableInfo.setColumnCount(columns.size());
            tableInfo.setColumns(columns);
            
            return tableInfo;
        } catch (Exception e) {
            logger.error("获取表 {} 信息失败: {}", tableName, e.getMessage());
            // 返回基本信息，避免整个操作失败
            TableInfo tableInfo = new TableInfo();
            tableInfo.setName(tableName);
            tableInfo.setRowCount(0);
            tableInfo.setColumnCount(0);
            tableInfo.setColumns(new ArrayList<>());
            return tableInfo;
        }
    }

    /**
     * 获取表数据（支持分页）
     */
    public Map<String, Object> getTableData(String databaseName, String tableName, int limit) {
        return getTableData(databaseName, tableName, limit, 0);
    }

    // 查询结果缓存
    private final ConcurrentHashMap<String, Object> queryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> queryCacheTime = new ConcurrentHashMap<>();
    private static final long QUERY_CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存

    // 表结构缓存
    private final ConcurrentHashMap<String, List<String>> tableStructureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tableStructureCacheTime = new ConcurrentHashMap<>();
    private static final long TABLE_STRUCTURE_CACHE_DURATION = 30 * 60 * 1000; // 30分钟缓存

    // COUNT查询缓存
    private final ConcurrentHashMap<String, Integer> countCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> countCacheTime = new ConcurrentHashMap<>();
    private static final long COUNT_CACHE_DURATION = 2 * 60 * 1000; // 2分钟缓存

    /**
     * 获取表数据（支持分页和偏移）- 性能优化版本
     */
    public Map<String, Object> getTableData(String databaseName, String tableName, int limit, int offset) {
        try {
            // 创建连接到指定数据库的JdbcTemplate
            DataSource dataSource = databaseConfig.createDataSourceForDatabase(databaseName);
            JdbcTemplate dbJdbcTemplate = new JdbcTemplate(dataSource);

            // 获取表结构（使用缓存）
            List<String> columns = getCachedTableStructure(dbJdbcTemplate, databaseName, tableName);

            // 获取总记录数（使用缓存）
            int totalCount = getCachedTableCount(dbJdbcTemplate, databaseName, tableName);

            // 优化的数据查询 - 避免SELECT *，只查询需要的列
            String dataSql = buildOptimizedDataQuery(tableName, columns, limit, offset);
            List<Map<String, Object>> rows = dbJdbcTemplate.queryForList(dataSql);

            // 添加OSS文件列
            List<String> enhancedColumns = new ArrayList<>(columns);
            enhancedColumns.add("图片");
            enhancedColumns.add("文本文件");

            // 批量处理OSS文件匹配 - 性能优化
            List<List<Object>> enhancedRows = new ArrayList<>();

            // 收集所有ID用于批量匹配
            List<String> allIds = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String id = getIdFromRow(row);
                allIds.add(id);
            }

            // 批量匹配图片和文本文件
            String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".jfif", ".jpe"};
            String[] textExtensions = {".txt", ".md", ".doc", ".docx", ".pdf", ".csv"};

            Map<String, String> imageMatches = ossService.batchMatchFiles(
                ossService.getOssConfig().getImageBucketName(), allIds, imageExtensions);
            Map<String, String> textMatches = ossService.batchMatchFiles(
                ossService.getOssConfig().getTextBucketName(), allIds, textExtensions);

            // 处理每一行数据，添加OSS文件信息
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                List<Object> enhancedRow = new ArrayList<>();

                // 添加原始数据
                for (String column : columns) {
                    enhancedRow.add(row.get(column));
                }

                // 获取当前行的ID
                String id = allIds.get(i);
                String imageFileName = imageMatches.get(id);
                String textFileName = textMatches.get(id);

                // 添加图片信息（异步加载模式）
                Map<String, Object> imageInfo = new HashMap<>();
                if (imageFileName != null) {
                    imageInfo.put("fileName", imageFileName);
                    imageInfo.put("exists", true); // 批量匹配已确认存在
                    imageInfo.put("loadAsync", true); // 标记为异步加载
                } else {
                    imageInfo.put("exists", false);
                }
                enhancedRow.add(imageInfo);

                // 添加文本文件信息（异步加载模式）
                Map<String, Object> textInfo = new HashMap<>();
                if (textFileName != null) {
                    textInfo.put("fileName", textFileName);
                    textInfo.put("exists", true); // 批量匹配已确认存在
                    textInfo.put("loadAsync", true); // 标记为异步加载
                } else {
                    textInfo.put("exists", false);
                }
                enhancedRow.add(textInfo);

                enhancedRows.add(enhancedRow);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("columns", enhancedColumns);
            result.put("data", enhancedRows);
            result.put("totalRows", enhancedRows.size());
            result.put("totalCount", totalCount);
            result.put("currentPage", offset / limit + 1);
            result.put("pageSize", limit);
            result.put("hasNext", offset + limit < totalCount);
            result.put("hasPrevious", offset > 0);
            result.put("tableName", tableName);
            result.put("databaseName", databaseName);

            logger.info("成功获取表 {}.{} 的 {} 条数据（包含OSS文件信息）", databaseName, tableName, enhancedRows.size());
            return result;
        } catch (Exception e) {
            logger.error("获取表数据失败: {}.{} - {}", databaseName, tableName, e.getMessage());
            throw new RuntimeException("获取表数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存的表结构
     */
    private List<String> getCachedTableStructure(JdbcTemplate jdbcTemplate, String databaseName, String tableName) {
        String cacheKey = databaseName + "." + tableName + ".structure";
        Long cacheTime = tableStructureCacheTime.get(cacheKey);

        // 检查缓存是否有效
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < TABLE_STRUCTURE_CACHE_DURATION) {
            List<String> cachedStructure = tableStructureCache.get(cacheKey);
            if (cachedStructure != null) {
                logger.debug("从缓存获取表结构: {}", cacheKey);
                return cachedStructure;
            }
        }

        // 查询表结构
        String describeSql = "DESCRIBE `" + tableName + "`";
        List<String> columns = jdbcTemplate.query(describeSql, (rs, rowNum) -> rs.getString("Field"));

        // 缓存结果
        tableStructureCache.put(cacheKey, columns);
        tableStructureCacheTime.put(cacheKey, System.currentTimeMillis());
        logger.debug("缓存表结构: {}, 列数: {}", cacheKey, columns.size());

        return columns;
    }

    /**
     * 获取缓存的表记录数
     */
    private int getCachedTableCount(JdbcTemplate jdbcTemplate, String databaseName, String tableName) {
        String cacheKey = databaseName + "." + tableName + ".count";
        Long cacheTime = countCacheTime.get(cacheKey);

        // 检查缓存是否有效
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < COUNT_CACHE_DURATION) {
            Integer cachedCount = countCache.get(cacheKey);
            if (cachedCount != null) {
                logger.debug("从缓存获取表记录数: {} = {}", cacheKey, cachedCount);
                return cachedCount;
            }
        }

        // 优化的COUNT查询 - 对于大表使用近似统计
        String countSql;
        try {
            // 首先尝试从information_schema获取近似行数（更快）
            countSql = "SELECT table_rows FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
            Integer approxCount = jdbcTemplate.queryForObject(countSql, Integer.class, databaseName, tableName);

            if (approxCount != null && approxCount > 0) {
                // 如果近似行数可用且合理，使用它
                countCache.put(cacheKey, approxCount);
                countCacheTime.put(cacheKey, System.currentTimeMillis());
                logger.debug("使用近似行数: {} = {}", cacheKey, approxCount);
                return approxCount;
            }
        } catch (Exception e) {
            logger.debug("获取近似行数失败，使用精确COUNT: {}", e.getMessage());
        }

        // 回退到精确COUNT查询
        countSql = "SELECT COUNT(*) FROM `" + tableName + "`";
        int exactCount = jdbcTemplate.queryForObject(countSql, Integer.class);

        // 缓存结果
        countCache.put(cacheKey, exactCount);
        countCacheTime.put(cacheKey, System.currentTimeMillis());
        logger.debug("缓存精确行数: {} = {}", cacheKey, exactCount);

        return exactCount;
    }

    /**
     * 构建优化的数据查询SQL
     */
    private String buildOptimizedDataQuery(String tableName, List<String> columns, int limit, int offset) {
        // 对于大偏移量，使用子查询优化
        if (offset > 10000) {
            // 假设第一列是主键或有索引的列
            String firstColumn = columns.get(0);
            return String.format(
                "SELECT * FROM `%s` WHERE `%s` >= (SELECT `%s` FROM `%s` ORDER BY `%s` LIMIT 1 OFFSET %d) ORDER BY `%s` LIMIT %d",
                tableName, firstColumn, firstColumn, tableName, firstColumn, offset, firstColumn, limit
            );
        } else {
            // 普通分页查询
            return String.format("SELECT * FROM `%s` LIMIT %d OFFSET %d", tableName, limit, offset);
        }
    }



    /**
     * 清理查询缓存
     */
    public void clearQueryCache() {
        queryCache.clear();
        queryCacheTime.clear();
        tableStructureCache.clear();
        tableStructureCacheTime.clear();
        countCache.clear();
        countCacheTime.clear();
        tableInfoCache.clear();
        tableInfoCacheTime.clear();
        logger.info("查询缓存已清理");
    }

    /**
     * 根据行数据查找对应的图片文件名
     * 匹配规则：只根据表中id字段匹配
     */
    private String findImageFileName(Map<String, Object> row, List<String> columns) {
        // 只获取id字段进行匹配
        String id = getIdFromRow(row);

        if (id != null && !id.isEmpty()) {
            logger.debug("尝试为ID {} 匹配图片文件", id);
            String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".jfif", ".jpe"};

            // 直接使用id作为文件名进行匹配
            for (String ext : imageExtensions) {
                String fileName = id + ext;
                if (ossService.doesImageExist(fileName)) {
                    logger.debug("找到图片文件: {}", fileName);
                    return fileName;
                }
            }
            logger.debug("未找到ID {} 对应的图片文件", id);
        } else {
            logger.debug("行数据中未找到有效的ID字段");
        }

        return null;
    }

    /**
     * 根据行数据查找对应的文本文件名
     * 匹配规则：只根据表中id字段匹配
     */
    private String findTextFileName(Map<String, Object> row, List<String> columns) {
        // 只获取id字段进行匹配
        String id = getIdFromRow(row);

        if (id != null && !id.isEmpty()) {
            logger.debug("尝试为ID {} 匹配文本文件", id);
            String[] textExtensions = {".txt", ".md", ".doc", ".docx", ".pdf", ".csv"};

            // 直接使用id作为文件名进行匹配
            for (String ext : textExtensions) {
                String fileName = id + ext;
                if (ossService.doesTextExist(fileName)) {
                    logger.debug("找到文本文件: {}", fileName);
                    return fileName;
                }
            }
            logger.debug("未找到ID {} 对应的文本文件", id);
        } else {
            logger.debug("行数据中未找到有效的ID字段");
        }

        return null;
    }

    /**
     * 检查是否是图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
               lowerCase.endsWith(".png") || lowerCase.endsWith(".gif") ||
               lowerCase.endsWith(".bmp") || lowerCase.endsWith(".webp") ||
               lowerCase.endsWith(".jfif") || lowerCase.endsWith(".jpe");
    }

    /**
     * 检查是否是文本文件
     */
    private boolean isTextFile(String fileName) {
        if (fileName == null) return false;
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".txt") || lowerCase.endsWith(".md") ||
               lowerCase.endsWith(".doc") || lowerCase.endsWith(".docx") ||
               lowerCase.endsWith(".pdf");
    }

    /**
     * 从行数据中获取名称字段
     */
    private String getNameFromRow(Map<String, Object> row) {
        // 尝试常见的名称字段
        String[] nameFields = {"name", "名称", "材料名称", "title", "标题", "产品名称", "品名"};

        for (String field : nameFields) {
            Object value = row.get(field);
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }

        return null;
    }

    /**
     * 从行数据中获取id字段（用于文件匹配）
     */
    private String getIdFromRow(Map<String, Object> row) {
        // 优先查找id字段
        String[] idFields = {"id", "ID"};

        for (String field : idFields) {
            Object value = row.get(field);
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }

        return null;
    }

    /**
     * 从行数据中获取序号字段（保留用于其他用途）
     */
    private String getNumberFromRow(Map<String, Object> row) {
        // 尝试常见的序号字段
        String[] numberFields = {"id", "ID", "序号", "编号", "number", "num", "code", "代码", "材料编号"};

        for (String field : numberFields) {
            Object value = row.get(field);
            if (value != null) {
                String strValue = value.toString().trim();
                if (!strValue.isEmpty()) {
                    return strValue;
                }
            }
        }

        return null;
    }

    /**
     * 生成可能的文件名组合
     */
    private List<String> generatePossibleFileNames(String name, String number) {
        List<String> possibleNames = new ArrayList<>();

        if (name != null && number != null) {
            // 名称_序号
            possibleNames.add(name + "_" + number);
            // 序号_名称
            possibleNames.add(number + "_" + name);
            // 名称-序号
            possibleNames.add(name + "-" + number);
            // 序号-名称
            possibleNames.add(number + "-" + name);
            // 名称序号（无分隔符）
            possibleNames.add(name + number);
            // 序号名称（无分隔符）
            possibleNames.add(number + name);
        }

        if (name != null) {
            possibleNames.add(name);
        }

        if (number != null) {
            possibleNames.add(number);
        }

        return possibleNames;
    }

    /**
     * 获取数据库统计信息
     */
    public DatabaseInfo getDatabaseStatistics(String databaseName) {
        try {
            List<TableInfo> tables = getTables(databaseName);

            int tableCount = tables.size();
            long totalRows = tables.stream().mapToLong(TableInfo::getRowCount).sum();

            DatabaseInfo dbInfo = new DatabaseInfo(databaseName, tableCount, totalRows);
            logger.info("数据库 {} 统计: {} 个表, {} 条记录", databaseName, tableCount, totalRows);

            return dbInfo;
        } catch (Exception e) {
            logger.error("获取数据库统计信息失败: {} - {}", databaseName, e.getMessage());
            return new DatabaseInfo(databaseName, 0, 0);
        }
    }

    /**
     * 导出表数据为CSV格式
     * 导出完整表数据，不分页
     */
    public String exportTableDataToCsv(String databaseName, String tableName) {
        try {
            // 创建连接到指定数据库的JdbcTemplate
            DataSource dataSource = databaseConfig.createDataSourceForDatabase(databaseName);
            JdbcTemplate dbJdbcTemplate = new JdbcTemplate(dataSource);

            // 获取表结构
            String describeSql = "DESCRIBE `" + tableName + "`";
            List<String> columns = dbJdbcTemplate.query(describeSql, (rs, rowNum) -> rs.getString("Field"));

            // 获取总记录数
            String countSql = "SELECT COUNT(*) FROM `" + tableName + "`";
            int totalCount = dbJdbcTemplate.queryForObject(countSql, Integer.class);

            // 获取所有数据（不分页）
            String dataSql = "SELECT * FROM `" + tableName + "`";
            List<Map<String, Object>> rows = dbJdbcTemplate.queryForList(dataSql);

            // 构建CSV内容
            StringBuilder csv = new StringBuilder();

            // 添加表头
            csv.append(String.join(",", columns)).append("\n");

            // 添加数据行
            for (Map<String, Object> row : rows) {
                List<String> csvRow = new ArrayList<>();
                for (String column : columns) {
                    Object value = row.get(column);
                    String cellValue = (value != null) ? value.toString() : "NULL";

                    // 处理包含逗号或引号的数据
                    if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                        cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                    }
                    csvRow.add(cellValue);
                }
                csv.append(String.join(",", csvRow)).append("\n");
            }

            logger.info("成功导出表 {}.{} 的完整数据，共 {} 条记录", databaseName, tableName, rows.size());
            return csv.toString();

        } catch (Exception e) {
            logger.error("导出表数据失败: {}.{} - {}", databaseName, tableName, e.getMessage());
            throw new RuntimeException("导出表数据失败: " + e.getMessage());
        }
    }
}
