package com.tal.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:26
 * @Description:
 */
@Configuration
public class OssConfig {

    @Value("${oss.endpoint}")
    private String endPoint;
    @Value("${oss.inner.endpoint}")
    private String innerPoint;
    @Value("${oss.accessKey}")
    private String accessKey;
    @Value("${oss.secretKey}")
    private String secretKey;
    @Value("${oss.bucketName}")
    private String bucketName;
    private long miniSize;

    public long getMiniSize() {
        return miniSize;
    }

    public void setMiniSize(long miniSize) {
        this.miniSize = miniSize;
    }

    public String getInnerPoint() {
        return innerPoint;
    }

    public void setInnerPoint(String innerPoint) {
        this.innerPoint = innerPoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

