package com.tal.upload.web.controller;

import com.alibaba.fastjson.JSON;
import com.tal.upload.service.FileUploadService;
import com.tal.upload.web.param.FilePartParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:18
 * @Description:
 */
@RestController
@RequestMapping("/file")
public class TesController {

    @Autowired
    private FileUploadService uploadService;
    @RequestMapping("/test")
    public String test(@RequestBody FilePartParam partParam){
        return JSON.toJSONString(partParam);
    }

    @RequestMapping("/upload1")
    public String uploadFile(FilePartParam partParam){

        System.out.println(JSON.toJSONString(partParam));
        try {
            byte[] bytes = partParam.getChunk().getBytes();
            uploadService.uploadFileByMappedByteBuffer(partParam.getFileName(),partParam.getChunkSize(),partParam.getPartIndex(),partParam.getTotalSize(),bytes);
        }catch (Exception e){
            e.printStackTrace();
        }

        return "ok";
    }
    @RequestMapping("/upload2")
    public String uploadFile2(FilePartParam partParam){

        try {
            byte[] bytes = partParam.getChunk().getBytes();
            uploadService.uploadFileRandomAccessFile(partParam.getFileName(),partParam.getChunkSize(),partParam.getPartIndex(),partParam.getTotalSize(),bytes);
        }catch (Exception e){
            e.printStackTrace();
        }

        return "ok";
    }
}
