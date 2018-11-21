package com.dearan.upload.service.impl;

import Listener.ProgressListener;
import client.OSSClient;
import com.alibaba.fastjson.JSON;
import com.dearan.upload.model.PartModel;
import com.dearan.upload.model.UploadModel;
import com.dearan.upload.redis.UploadRedisService;
import com.dearan.upload.service.OssService;
import lombok.extern.log4j.Log4j2;
import model.CompleteMultipartUploadResult;
import model.PartETag;
import model.PartListing;
import model.PartSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import request.CompleteMultipartUploadRequest;
import request.ListPartsRequest;
import request.UploadPartRequest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:48
 * @Description:
 */

@Log4j2
@Service
public class OssServiceImpl implements OssService {


    @Autowired
    private UploadRedisService uploadRedisService;

    private static Map<String,Map<Integer,PartModel>> fileCache = new HashMap<>();
    @Override

    public boolean uploadMinFile(UploadModel model) {

        log.info("uploadMinFile : start --> 【param:{}】", JSON.toJSON(model));
        OSSClient ossClient = null;
        try {
            ossClient = new OSSClient(model.getEndPoint(), model.getAccessKey(), model.getSecretKey());
            ossClient.putObject(model.getBucketName(), model.getObjName(), new ByteArrayInputStream(model.getFile()));
            log.info("uploadMinFile : end --> 【param:{}】", JSON.toJSON(model));
            return true;
        } catch (Exception e) {
            log.error("上传文件异常", e);
        } finally {
            if (ossClient != null) {
                ossClient.close();
            }
        }
        return false;
    }

    @Override
    public boolean uploadFileByPart(OSSClient ossClient, PartModel model) {

        log.info("uploadFileByPart : start --> 【param : {}】",JSON.toJSONString(model));
        Integer uploadIndex = uploadRedisService.getPartId(model.getUploadId());
        //第一个分片或者已上传分片的连续分片
        if (model.getPartId() == 0 || (uploadIndex != null && model.getPartId()==uploadIndex+1)) {
            boolean result = this.uploadPartFile(ossClient, model);
            log.info("uploadFileByPart : end --> 【param : {}】",JSON.toJSONString(model));
            return result;
        }else {
            //分片编号不连续
            if (null == uploadIndex || model.getPartId() > uploadIndex+1) {

                //上传缓存中的数据
                this.uploadCacheFile(ossClient,model.getUploadId(),model.getPartId());

                //重新获取已上传分片编号
                uploadIndex = uploadRedisService.getPartId(model.getUploadId());
                if (uploadIndex+1 != model.getPartId()){
                    //放到本地缓存
                    Map<Integer,PartModel> cache = new HashMap<>(1);
                    cache.put(model.getPartId(),model);
                    log.info("uploadFileByPart : 暂存到cache --> 【param : {}】",JSON.toJSONString(model));
                    fileCache.put(model.getUploadId(),cache);
                }else {
                    this.uploadPartFile(ossClient,model);
                }
                log.info("uploadFileByPart : end --> 【param : {}】",JSON.toJSONString(model));
                return true;
            }
        }
        log.info("uploadFileByPart : end 文件上传失败--> 【param : {}】",JSON.toJSONString(model));
        return false;
    }

    /**
     * 上传分片
     * @param ossClient
     * @param model
     * @return
     */
    private boolean uploadPartFile(OSSClient ossClient, PartModel model){
        try {
            ossClient.uploadPart(new UploadPartRequest(model.getBucketName(), model.getObjName(), model.getUploadId(), model.getPartId(), new ByteArrayInputStream(model.getFilePart()),model.getTotalPart() ).withProgressListener(new ProgressListener() {
                @Override
                public void progress(long transferred, long total, float percentage) {
                    if (transferred == total){
                        log.info("uploadPartFile : 分片上传完成 -->【uploadId : {},partId :{} 】",model.getUploadId(),model.getPartId());
                    }
                }
            }));
            //每上传成功一个分片更新进度信息
            uploadRedisService.setPartId(model.getUploadId(),model.getPartId());

            //最后一个分片上传完成
            if (model.isLast()){
                this.finishUpload(ossClient,model);
            }
            return true;
        }catch (Exception e){
            log.info("上传分片异常",e);
        }
        return false;
    }

    /**
     *查询缓存并上传连续的分片
     * @param ossClient
     * @param uploadId
     * @param partId  已上传的最后一个分片编号
     */
    private void uploadCacheFile(OSSClient ossClient,String uploadId,int partId){

        log.info("uploadCacheFile : start --> 【uploadId : {},partId :{} 】",uploadId,partId);
        //循环遍历读取上传连续的缓存分片数据
        while(true){
            partId++;
            PartModel partModel = fileCache.get(uploadId).get(partId);
            if (null == partModel){
                log.info("未查询到缓存中的连续分片 -->【uploadId : {},partId :{} 】",uploadId,partId);
                break;
            }else {
                log.info("获取到连续缓存分片 : upload --> 【partModel : {}】",JSON.toJSONString(partModel));
                boolean uploadResult = uploadPartFile(ossClient, partModel);
                //缓存上传成功
                if (uploadResult){
                    uploadRedisService.setPartId(uploadId,partId);
                    //移除缓存数据
                    fileCache.get(uploadId).remove(partId);
                }
            }
        }
        log.info("uploadCacheFile : end --> 【uploadId : {},partId :{} 】",uploadId,partId);
    }

    /**
     * 完成上传分片任务
     * @param ossClient
     * @param model
     * @return
     */
    private boolean finishUpload(OSSClient ossClient, PartModel model) {
        ListPartsRequest listPartsRequest = new ListPartsRequest(model.getBucketName(), model.getObjName(), model.getUploadId());
        PartListing partListing = null;
        try {
            try {
                partListing = ossClient.listParts(listPartsRequest);
            }catch (Exception e){
                log.error("获取上传分片列表异常",e);
            }
            if (null == partListing){
                return false;
            }else {
                List<PartSummary> parts = partListing.getParts();
                List<PartETag> partETags = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    PartSummary part = parts.get(i);
                    PartETag partETag = new PartETag(i + 1, part.getETag());
                    partETags.add(partETag);
                }
                CompleteMultipartUploadRequest completeUploadRequest = new CompleteMultipartUploadRequest(model.getBucketName(), model.getObjName(), model.getUploadId(), partETags);
                CompleteMultipartUploadResult completeMultipartUploadResult = null;
                try {
                    completeMultipartUploadResult = ossClient.completeMultipartUpload(completeUploadRequest);
                }catch (Exception e){
                    log.error("completeMultipartUpload 异常",e);
                }
                if (null == completeMultipartUploadResult){
                    return false;
                }else {
                    log.info("---------完成分片上传-------------");
                    return true;
                }
            }
        }finally {
            if (ossClient != null) {
                ossClient.close();
            }
        }
    }
}
