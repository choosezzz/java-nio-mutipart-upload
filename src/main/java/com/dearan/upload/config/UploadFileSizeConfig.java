package com.dearan.upload.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;
import java.io.File;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/11  20:09
 * @Description:
 */
@Configuration
public class UploadFileSizeConfig {

    /**
     * 文件上传配置
     * @return
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //文件最大KB,MB
        factory.setMaxFileSize("5120MB");
        /// 设置总上传数据总大小
        factory.setMaxRequestSize("5120MB");
        //设置上传临时文件目录
//        factory.setLocation("/data/tmp");

        return factory.createMultipartConfig();
    }
}
