package com.material.management.controller;

import com.material.management.model.DatabaseInfo;
import com.material.management.model.TableInfo;
import com.material.management.service.DatabaseService;
import com.material.management.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API控制器
 * 提供JSON格式的数据接口
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private OssService ossService;

    /**
     * 测试数据库连接
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isConnected = databaseService.testConnection();
            response.put("success", isConnected);
            response.put("message", isConnected ? "数据库连接成功" : "数据库连接失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "连接测试异常: " + e.getMessage());
            logger.error("数据库连接测试异常: {}", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取数据库列表
     */
    @GetMapping("/databases")
    public ResponseEntity<Map<String, Object>> getDatabases() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DatabaseInfo> databases = databaseService.getDatabases();
            response.put("success", true);
            response.put("data", databases);
            response.put("count", databases.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取数据库列表失败: " + e.getMessage());
            logger.error("获取数据库列表失败: {}", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取指定数据库的表列表
     */
    @GetMapping("/databases/{databaseName}/tables")
    public ResponseEntity<Map<String, Object>> getTables(@PathVariable String databaseName) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<TableInfo> tables = databaseService.getTables(databaseName);
            response.put("success", true);
            response.put("data", tables);
            response.put("count", tables.size());
            response.put("databaseName", databaseName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取表列表失败: " + e.getMessage());
            logger.error("获取数据库 {} 的表列表失败: {}", databaseName, e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取表数据（支持分页）- 优化版本
     */
    @GetMapping("/databases/{databaseName}/tables/{tableName}/data")
    public ResponseEntity<Map<String, Object>> getTableData(
            @PathVariable String databaseName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "50") int limit,  // 减少默认页面大小
            @RequestParam(defaultValue = "0") int offset) {

        Map<String, Object> response = new HashMap<>();
        try {
            // 限制最大查询数量 - 更严格的限制
            if (limit > 1000) {
                limit = 1000;
            }

            Map<String, Object> tableData = databaseService.getTableData(databaseName, tableName, limit, offset);
            response.put("success", true);
            response.put("data", tableData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取表数据失败: " + e.getMessage());
            logger.error("获取表数据失败 {}.{}: {}", databaseName, tableName, e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }



    /**
     * 获取数据库统计信息
     */
    @GetMapping("/databases/{databaseName}/statistics")
    public ResponseEntity<Map<String, Object>> getDatabaseStatistics(@PathVariable String databaseName) {
        Map<String, Object> response = new HashMap<>();
        try {
            DatabaseInfo dbInfo = databaseService.getDatabaseStatistics(databaseName);
            response.put("success", true);
            response.put("data", dbInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取数据库统计信息失败: " + e.getMessage());
            logger.error("获取数据库 {} 统计信息失败: {}", databaseName, e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取图片完整内容
     */
    @GetMapping("/oss/image/{fileName}")
    public ResponseEntity<Map<String, Object>> getFullImage(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageBase64 = ossService.getImageFullBase64(fileName);
            if (imageBase64 != null) {
                response.put("success", true);
                response.put("data", imageBase64);
                response.put("fileName", fileName);
            } else {
                response.put("success", false);
                response.put("message", "图片不存在或无法加载");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取图片失败: " + e.getMessage());
            logger.error("获取图片失败 {}: {}", fileName, e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取文本文件完整内容
     */
    @GetMapping("/oss/text/{fileName}")
    public ResponseEntity<Map<String, Object>> getFullText(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 检查是否为CSV文件
            if (fileName.toLowerCase().endsWith(".csv")) {
                // 返回CSV解析结果
                Map<String, Object> csvResult = ossService.getCsvContent(fileName);
                return ResponseEntity.ok(csvResult);
            } else {
                // 处理普通文本文件
                String textContent = ossService.getTextFullContent(fileName);
                if (textContent != null) {
                    response.put("success", true);
                    response.put("data", textContent);
                    response.put("fileName", fileName);
                    response.put("fileType", "text");
                } else {
                    response.put("success", false);
                    response.put("message", "文本文件不存在或无法加载");
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取文本文件失败: " + e.getMessage());
            logger.error("获取文本文件失败 {}: {}", fileName, e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 专门获取CSV文件内容的API端点
     */
    @GetMapping("/oss/csv/{fileName}")
    public ResponseEntity<Map<String, Object>> getCsvContent(@PathVariable String fileName) {
        try {
            Map<String, Object> csvResult = ossService.getCsvContent(fileName);
            return ResponseEntity.ok(csvResult);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取CSV文件失败: " + e.getMessage());
            logger.error("获取CSV文件失败 {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 异步获取图片缩略图
     */
    @GetMapping("/oss/thumbnail/{fileName}")
    public ResponseEntity<Map<String, Object>> getThumbnail(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String thumbnail = ossService.getImageThumbnailBase64(fileName);
            if (thumbnail != null) {
                response.put("success", true);
                response.put("data", thumbnail);
                response.put("fileName", fileName);
            } else {
                response.put("success", false);
                response.put("message", "缩略图生成失败");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取缩略图失败: " + e.getMessage());
            logger.error("获取缩略图失败 {}: {}", fileName, e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 异步获取文本预览
     */
    @GetMapping("/oss/preview/{fileName}")
    public ResponseEntity<Map<String, Object>> getTextPreview(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String preview = ossService.getTextPreview(fileName);
            response.put("success", true);
            response.put("data", preview);
            response.put("fileName", fileName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取文本预览失败: " + e.getMessage());
            logger.error("获取文本预览失败 {}: {}", fileName, e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 下载图片文件
     */
    @GetMapping("/oss/download/image/{fileName}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String fileName) {
        try {
            byte[] imageData = ossService.downloadImageFile(fileName);
            if (imageData != null) {
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                        .header("Content-Type", "application/octet-stream")
                        .body(imageData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("下载图片文件失败 {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 下载文本文件
     */
    @GetMapping("/oss/download/text/{fileName}")
    public ResponseEntity<byte[]> downloadText(@PathVariable String fileName) {
        try {
            byte[] textData = ossService.downloadTextFile(fileName);
            if (textData != null) {
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                        .header("Content-Type", "application/octet-stream")
                        .body(textData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("下载文本文件失败 {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 下载完整表数据为CSV格式
     */
    @GetMapping("/databases/{databaseName}/tables/{tableName}/download/csv")
    public ResponseEntity<byte[]> downloadTableDataAsCsv(
            @PathVariable String databaseName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "false") boolean fullData) {
        try {
            String csvContent;
            String fileName;

            if (fullData) {
                // 获取完整表数据
                csvContent = databaseService.exportTableDataToCsv(databaseName, tableName);
                fileName = String.format("%s_%s_complete_data.csv", databaseName, tableName);
            } else {
                // 获取当前页面数据（默认100条）
                Map<String, Object> tableData = databaseService.getTableData(databaseName, tableName, 100);
                csvContent = convertTableDataToCsv(tableData);
                fileName = String.format("%s_%s_current_page.csv", databaseName, tableName);
            }

            byte[] csvBytes = csvContent.getBytes("UTF-8");

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", "text/csv; charset=utf-8")
                    .body(csvBytes);

        } catch (Exception e) {
            logger.error("下载表数据失败 {}.{}: {}", databaseName, tableName, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 将表数据转换为CSV格式
     */
    private String convertTableDataToCsv(Map<String, Object> tableData) {
        StringBuilder csv = new StringBuilder();

        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) tableData.get("columns");
        @SuppressWarnings("unchecked")
        List<List<Object>> data = (List<List<Object>>) tableData.get("data");

        // 添加表头
        csv.append(String.join(",", columns)).append("\n");

        // 添加数据行
        for (List<Object> row : data) {
            List<String> csvRow = new ArrayList<>();
            for (Object cell : row) {
                String cellValue = "";
                if (cell != null) {
                    if (cell instanceof Map) {
                        // 处理OSS文件信息
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fileInfo = (Map<String, Object>) cell;
                        Object fileName = fileInfo.get("fileName");
                        Boolean exists = (Boolean) fileInfo.get("exists");
                        if (fileName != null && Boolean.TRUE.equals(exists)) {
                            cellValue = fileName.toString();
                        }
                    } else {
                        cellValue = cell.toString();
                    }
                }

                // 处理包含逗号或引号的数据
                if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                    cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                }
                csvRow.add(cellValue);
            }
            csv.append(String.join(",", csvRow)).append("\n");
        }

        return csv.toString();
    }


}
