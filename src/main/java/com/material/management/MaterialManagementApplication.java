package com.material.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 材料数据管理系统主应用类
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@SpringBootApplication
public class MaterialManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialManagementApplication.class, args);
        System.out.println("=================================");
        System.out.println("材料数据管理系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("=================================");
    }
}
