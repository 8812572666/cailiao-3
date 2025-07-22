package com.material.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfig {
    
    private String accessKeyId = "LTAI5tAzmjQ8GBZDocozoBSy";
    private String accessKeySecret = "42cL9W2JOvMuYgTVGAES6Hi2593BjP";
    private String endpoint = "oss-cn-wuhan-lr.aliyuncs.com";
    private String textBucketName = "testcxf";
    private String imageBucketName = "tupian-cxf";
    
    // 缩略图配置
    private int thumbnailWidth = 150;
    private int thumbnailHeight = 150;
    private String thumbnailFormat = "jpg";
    
    // 文本预览配置
    private int textPreviewLength = 100;
    
    public OssConfig() {}

    // Getters and Setters
    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTextBucketName() {
        return textBucketName;
    }

    public void setTextBucketName(String textBucketName) {
        this.textBucketName = textBucketName;
    }

    public String getImageBucketName() {
        return imageBucketName;
    }

    public void setImageBucketName(String imageBucketName) {
        this.imageBucketName = imageBucketName;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    public String getThumbnailFormat() {
        return thumbnailFormat;
    }

    public void setThumbnailFormat(String thumbnailFormat) {
        this.thumbnailFormat = thumbnailFormat;
    }

    public int getTextPreviewLength() {
        return textPreviewLength;
    }

    public void setTextPreviewLength(int textPreviewLength) {
        this.textPreviewLength = textPreviewLength;
    }

    @Override
    public String toString() {
        return "OssConfig{" +
                "accessKeyId='" + accessKeyId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", textBucketName='" + textBucketName + '\'' +
                ", imageBucketName='" + imageBucketName + '\'' +
                ", thumbnailWidth=" + thumbnailWidth +
                ", thumbnailHeight=" + thumbnailHeight +
                ", thumbnailFormat='" + thumbnailFormat + '\'' +
                ", textPreviewLength=" + textPreviewLength +
                '}';
    }
}
