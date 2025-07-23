package com.material.management.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.material.management.config.OssConfig;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS服务类
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@Service
public class OssService {
    
    private static final Logger logger = LoggerFactory.getLogger(OssService.class);
    
    @Autowired
    private OssConfig ossConfig;

    private OSS ossClient;

    // 缓存机制
    private final ConcurrentHashMap<String, String> thumbnailCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> textPreviewCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> fileExistenceCache = new ConcurrentHashMap<>();

    // 文件列表缓存 - 用于批量匹配优化
    private final Map<String, Set<String>> bucketFileListCache = new ConcurrentHashMap<>();
    private final Map<String, Long> bucketFileListCacheTime = new ConcurrentHashMap<>();
    private static final long FILE_LIST_CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
    
    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(
                "https://" + ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
            );
            logger.info("OSS客户端初始化成功");
        } catch (Exception e) {
            logger.error("OSS客户端初始化失败: {}", e.getMessage());
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            logger.info("OSS客户端已关闭");
        }
    }
    
    /**
     * 获取图片缩略图的Base64编码（带缓存和超时处理）
     */
    public String getImageThumbnailBase64(String fileName) {
        // 先检查缓存
        String cached = thumbnailCache.get(fileName);
        if (cached != null) {
            logger.debug("从缓存获取图片缩略图: {}", fileName);
            return cached;
        }

        // 检查文件是否存在（使用缓存）
        if (!doesImageExist(fileName)) {
            logger.debug("图片文件不存在: {}", fileName);
            return null;
        }

        try {
            logger.debug("开始生成图片缩略图: {}", fileName);
            long startTime = System.currentTimeMillis();

            OSSObject ossObject = ossClient.getObject(ossConfig.getImageBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            // 生成缩略图
            ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream();
            Thumbnails.of(inputStream)
                    .size(ossConfig.getThumbnailWidth(), ossConfig.getThumbnailHeight())
                    .outputFormat(ossConfig.getThumbnailFormat())
                    .toOutputStream(thumbnailOutput);

            // 转换为Base64
            byte[] thumbnailBytes = thumbnailOutput.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(thumbnailBytes);
            String result = "data:image/" + ossConfig.getThumbnailFormat() + ";base64," + base64;

            inputStream.close();
            thumbnailOutput.close();

            // 缓存结果
            thumbnailCache.put(fileName, result);

            long endTime = System.currentTimeMillis();
            logger.debug("图片缩略图生成完成: {}, 耗时: {}ms", fileName, endTime - startTime);

            return result;

        } catch (Exception e) {
            logger.error("获取图片缩略图失败: {}, 错误: {}", fileName, e.getMessage());
            // 缓存失败结果，避免重复尝试
            thumbnailCache.put(fileName, null);
            return null;
        }
    }
    
    /**
     * 获取图片完整内容的Base64编码
     */
    public String getImageFullBase64(String fileName) {
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getImageBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();
            
            byte[] imageBytes = IOUtils.toByteArray(inputStream);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            inputStream.close();
            
            // 根据文件扩展名确定MIME类型
            String mimeType = getMimeType(fileName);
            return "data:" + mimeType + ";base64," + base64;
            
        } catch (Exception e) {
            logger.error("获取完整图片失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取文本文件预览内容（带缓存和超时处理）
     */
    public String getTextPreview(String fileName) {
        // 先检查缓存
        String cached = textPreviewCache.get(fileName);
        if (cached != null) {
            logger.debug("从缓存获取文本预览: {}", fileName);
            return cached;
        }

        // 检查文件是否存在（使用缓存）
        if (!doesTextExist(fileName)) {
            logger.debug("文本文件不存在: {}", fileName);
            return null;
        }

        try {
            logger.debug("开始获取文本预览: {}", fileName);
            long startTime = System.currentTimeMillis();

            OSSObject ossObject = ossClient.getObject(ossConfig.getTextBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            // 截取预览长度
            String result;
            if (content.length() > ossConfig.getTextPreviewLength()) {
                result = content.substring(0, ossConfig.getTextPreviewLength()) + "...";
            } else {
                result = content;
            }

            // 缓存结果
            textPreviewCache.put(fileName, result);

            long endTime = System.currentTimeMillis();
            logger.debug("文本预览获取完成: {}, 耗时: {}ms", fileName, endTime - startTime);

            return result;

        } catch (Exception e) {
            logger.error("获取文本预览失败: {}, 错误: {}", fileName, e.getMessage());
            // 缓存失败结果，避免重复尝试
            textPreviewCache.put(fileName, null);
            return null;
        }
    }
    
    /**
     * 获取文本文件完整内容
     */
    public String getTextFullContent(String fileName) {
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getTextBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            return content;

        } catch (Exception e) {
            logger.error("获取完整文本内容失败: {}", e.getMessage());
            return "无法加载文件内容";
        }
    }

    /**
     * 获取CSV文件内容并转换为HTML表格格式
     */
    public Map<String, Object> getCsvContent(String fileName) {
        Map<String, Object> result = new HashMap<>();
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getTextBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            // 解析CSV内容
            String[] lines = content.split("\n");
            if (lines.length == 0) {
                result.put("success", false);
                result.put("message", "CSV文件为空");
                return result;
            }

            List<List<String>> csvData = new ArrayList<>();
            List<String> headers = null;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                // 简单的CSV解析（处理逗号分隔）
                List<String> row = parseCsvLine(line);

                if (i == 0) {
                    headers = row; // 第一行作为表头
                } else {
                    csvData.add(row);
                }
            }

            result.put("success", true);
            result.put("headers", headers);
            result.put("data", csvData);
            result.put("rowCount", csvData.size());
            result.put("fileName", fileName);

            logger.debug("CSV文件解析完成: {}, 行数: {}", fileName, csvData.size());
            return result;

        } catch (Exception e) {
            logger.error("获取CSV文件内容失败: {}, 错误: {}", fileName, e.getMessage());
            result.put("success", false);
            result.put("message", "无法加载CSV文件: " + e.getMessage());
            return result;
        }
    }

    /**
     * 解析CSV行（简单实现，处理基本的逗号分隔和引号包围）
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString().trim());
        return result;
    }
    
    /**
     * 检查文件是否存在（带缓存）
     */
    public boolean doesObjectExist(String bucketName, String fileName) {
        String cacheKey = bucketName + ":" + fileName;
        Boolean cached = fileExistenceCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            boolean exists = ossClient.doesObjectExist(bucketName, fileName);
            // 缓存结果
            fileExistenceCache.put(cacheKey, exists);
            return exists;
        } catch (Exception e) {
            logger.error("检查文件存在性失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查图片文件是否存在
     */
    public boolean doesImageExist(String fileName) {
        return doesObjectExist(ossConfig.getImageBucketName(), fileName);
    }
    
    /**
     * 检查文本文件是否存在
     */
    public boolean doesTextExist(String fileName) {
        return doesObjectExist(ossConfig.getTextBucketName(), fileName);
    }
    
    /**
     * 根据文件扩展名获取MIME类型
     */
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }
    
    /**
     * 列出桶中的所有文件
     */
    public List<String> listFiles(String bucketName) {
        List<String> fileNames = new ArrayList<>();
        try {
            ObjectListing objectListing = ossClient.listObjects(bucketName);
            for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                fileNames.add(objectSummary.getKey());
            }
        } catch (Exception e) {
            logger.error("列出文件失败: {}", e.getMessage());
        }
        return fileNames;
    }

    /**
     * 获取桶中的文件列表（带缓存）
     */
    public Set<String> getCachedFileList(String bucketName) {
        String cacheKey = bucketName;
        Long cacheTime = bucketFileListCacheTime.get(cacheKey);

        // 检查缓存是否有效
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < FILE_LIST_CACHE_DURATION) {
            Set<String> cachedList = bucketFileListCache.get(cacheKey);
            if (cachedList != null) {
                logger.debug("从缓存获取文件列表: {}, 文件数: {}", bucketName, cachedList.size());
                return cachedList;
            }
        }

        // 缓存过期或不存在，重新获取
        try {
            logger.debug("重新获取文件列表: {}", bucketName);
            Set<String> fileSet = new HashSet<>();
            ObjectListing objectListing = ossClient.listObjects(bucketName);
            for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                fileSet.add(objectSummary.getKey());
            }

            // 更新缓存
            bucketFileListCache.put(cacheKey, fileSet);
            bucketFileListCacheTime.put(cacheKey, System.currentTimeMillis());

            logger.debug("文件列表缓存已更新: {}, 文件数: {}", bucketName, fileSet.size());
            return fileSet;
        } catch (Exception e) {
            logger.error("获取文件列表失败: {}", e.getMessage());
            return new HashSet<>();
        }
    }
    
    /**
     * 列出图片桶中的所有文件
     */
    public List<String> listImageFiles() {
        return listFiles(ossConfig.getImageBucketName());
    }
    
    /**
     * 列出文本桶中的所有文件
     */
    public List<String> listTextFiles() {
        return listFiles(ossConfig.getTextBucketName());
    }

    /**
     * 下载图片文件
     */
    public byte[] downloadImageFile(String fileName) {
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getImageBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            byte[] fileData = IOUtils.toByteArray(inputStream);
            inputStream.close();

            logger.info("图片文件下载成功: {}", fileName);
            return fileData;

        } catch (Exception e) {
            logger.error("下载图片文件失败: {}, 错误: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 下载文本文件
     */
    public byte[] downloadTextFile(String fileName) {
        try {
            OSSObject ossObject = ossClient.getObject(ossConfig.getTextBucketName(), fileName);
            InputStream inputStream = ossObject.getObjectContent();

            byte[] fileData = IOUtils.toByteArray(inputStream);
            inputStream.close();

            logger.info("文本文件下载成功: {}", fileName);
            return fileData;

        } catch (Exception e) {
            logger.error("下载文本文件失败: {}, 错误: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 批量检查文件是否存在（优化版本）
     */
    public Map<String, Boolean> batchCheckFilesExist(String bucketName, List<String> fileNames) {
        Map<String, Boolean> results = new HashMap<>();
        Set<String> fileList = getCachedFileList(bucketName);

        for (String fileName : fileNames) {
            boolean exists = fileList.contains(fileName);
            results.put(fileName, exists);

            // 同时更新单文件缓存
            String cacheKey = bucketName + ":" + fileName;
            fileExistenceCache.put(cacheKey, exists);
        }

        return results;
    }

    /**
     * 批量匹配文件（根据ID和扩展名列表）- 性能优化版本
     */
    public Map<String, String> batchMatchFiles(String bucketName, List<String> ids, String[] extensions) {
        Map<String, String> results = new HashMap<>();
        Set<String> fileList = getCachedFileList(bucketName);

        // 使用并行流提高大批量处理性能
        if (ids.size() > 100) {
            Map<String, String> parallelResults = ids.parallelStream()
                .filter(id -> id != null && !id.isEmpty())
                .collect(HashMap::new,
                    (map, id) -> {
                        String matchedFile = findMatchingFile(fileList, id, extensions);
                        map.put(id, matchedFile);
                    },
                    HashMap::putAll);
            results.putAll(parallelResults);
        } else {
            // 小批量使用串行处理
            for (String id : ids) {
                if (id != null && !id.isEmpty()) {
                    String matchedFile = findMatchingFile(fileList, id, extensions);
                    results.put(id, matchedFile);
                }
            }
        }

        logger.debug("批量匹配文件完成: {} 个ID，匹配到 {} 个文件", ids.size(),
                    results.values().stream().mapToInt(v -> v != null ? 1 : 0).sum());
        return results;
    }

    /**
     * 查找匹配的文件
     */
    private String findMatchingFile(Set<String> fileList, String id, String[] extensions) {
        for (String ext : extensions) {
            String fileName = id + ext;
            if (fileList.contains(fileName)) {
                return fileName;
            }
        }
        return null;
    }

    /**
     * 获取OSS配置（用于其他服务访问）
     */
    public OssConfig getOssConfig() {
        return ossConfig;
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        thumbnailCache.clear();
        textPreviewCache.clear();
        fileExistenceCache.clear();
        bucketFileListCache.clear();
        bucketFileListCacheTime.clear();
        logger.info("OSS缓存已清理");
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("缓存统计 - 缩略图: %d, 文本预览: %d, 文件存在性: %d",
                thumbnailCache.size(), textPreviewCache.size(), fileExistenceCache.size());
    }
}
