package com.material.management.controller;

import com.material.management.model.DatabaseInfo;
import com.material.management.model.TableInfo;
import com.material.management.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 主控制器
 * 处理页面路由和数据展示
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private DatabaseService databaseService;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        try {
            // 测试数据库连接
            boolean connectionStatus = databaseService.testConnection();
            model.addAttribute("connectionStatus", connectionStatus);
            
            if (connectionStatus) {
                // 获取数据库列表
                List<DatabaseInfo> databases = databaseService.getDatabases();
                model.addAttribute("databases", databases);
                logger.info("首页加载成功，找到 {} 个数据库", databases.size());
            } else {
                model.addAttribute("errorMessage", "数据库连接失败，请检查配置");
                logger.error("首页加载失败：数据库连接失败");
            }
        } catch (Exception e) {
            model.addAttribute("connectionStatus", false);
            model.addAttribute("errorMessage", "系统错误: " + e.getMessage());
            logger.error("首页加载异常: {}", e.getMessage());
        }
        
        return "index";
    }

    /**
     * 数据库详情页面
     */
    @GetMapping("/database/{databaseName}")
    public String databaseDetail(@PathVariable String databaseName, Model model) {
        try {
            // 获取数据库统计信息
            DatabaseInfo dbInfo = databaseService.getDatabaseStatistics(databaseName);
            model.addAttribute("databaseInfo", dbInfo);
            
            // 获取表列表
            List<TableInfo> tables = databaseService.getTables(databaseName);
            model.addAttribute("tables", tables);
            model.addAttribute("databaseName", databaseName);
            
            logger.info("数据库 {} 详情页加载成功，包含 {} 个表", databaseName, tables.size());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "获取数据库信息失败: " + e.getMessage());
            logger.error("数据库 {} 详情页加载失败: {}", databaseName, e.getMessage());
        }
        
        return "database-detail";
    }

    /**
     * 表数据页面
     */
    @GetMapping("/database/{databaseName}/table/{tableName}")
    public String tableData(
            @PathVariable String databaseName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "100") int limit,
            Model model) {
        try {
            // 限制最大查询数量
            if (limit > 100000) {
                limit = 100000;
            }
            
            // 获取表数据
            Map<String, Object> tableData = databaseService.getTableData(databaseName, tableName, limit);
            model.addAttribute("tableData", tableData);
            model.addAttribute("databaseName", databaseName);
            model.addAttribute("tableName", tableName);
            model.addAttribute("limit", limit);
            
            // 获取表信息
            List<TableInfo> tables = databaseService.getTables(databaseName);
            TableInfo currentTable = tables.stream()
                    .filter(t -> t.getName().equals(tableName))
                    .findFirst()
                    .orElse(null);
            model.addAttribute("tableInfo", currentTable);
            
            logger.info("表数据页 {}.{} 加载成功，显示 {} 条记录", 
                       databaseName, tableName, tableData.get("totalRows"));
        } catch (Exception e) {
            model.addAttribute("errorMessage", "获取表数据失败: " + e.getMessage());
            logger.error("表数据页 {}.{} 加载失败: {}", databaseName, tableName, e.getMessage());
        }
        
        return "table-data";
    }
}
