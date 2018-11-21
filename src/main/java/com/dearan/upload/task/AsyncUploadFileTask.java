package com.dearan.upload.task;

import Listener.ProgressListener;
import client.OSSClient;
import com.dearan.upload.model.PartModel;
import com.dearan.upload.config.OssConfig;
import com.dearan.upload.service.OssService;
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import request.CompleteMultipartUploadRequest;
import request.InitiateMultipartUploadRequest;
import request.ListPartsRequest;
import request.UploadPartRequest;
import util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/12  19:19
 * @Description:
 */
@Component
public class  AsyncUploadFileTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncUploadFileTask.class);

    private static final int MINI_SIZE = 100 * 1024 * 1024;
    private static final long PART_SIZE = 10 * 1024 * 1024;
    @Autowired
    private OssService ossService;
    @Autowired
    private OssConfig ossConfig;

    @Async("AsyncTaskExecutor")
    public void asyncUploadFile(String bucketName, String objKey, byte[] file) {

        logger.info("asyncUploadFile : start --> 【bucketName : {},objKey : {},file size : {}】",bucketName,objKey,file.length);
        OSSClient ossClient = new OSSClient(ossConfig.getEndPoint(), ossConfig.getAccessKey(), ossConfig.getSecretKey());

        try {
            if (file.length <= MINI_SIZE) {
                try {
                    ossClient.putObject(bucketName, objKey, new ByteArrayInputStream(file));
                    logger.info("asyncUploadFile : end --> 【bucketName : {},objKey : {},file size : {}】",bucketName,objKey,file.length);
                } catch (Exception e) {
                    logger.error("上传文件异常", e);
                }
            } else {
                InitiateMultipartUploadResult result = null;
                InputStream fileInputStream = null;
                //初始化上传分片事件
                try {
                    result = ossClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, objKey));
                    ArrayList<IOUtils.UploadPart> uploadParts = IOUtils.splitFile(file.length, PART_SIZE);
                    //将上传分片进行排序
                    Collections.sort(uploadParts, new Comparator<IOUtils.UploadPart>() {
                        @Override
                        public int compare(IOUtils.UploadPart o1, IOUtils.UploadPart o2) {
                            return o2.number - o1.number;
                        }
                    });

                    for (IOUtils.UploadPart uploadPart : uploadParts) {
                        fileInputStream = new ByteArrayInputStream(file);
                        fileInputStream.skip(uploadPart.offset);
                        ossClient.uploadPart(new UploadPartRequest(bucketName, objKey, result.getUploadId(), uploadPart.number, fileInputStream, uploadPart.size).withProgressListener(new ProgressListener() {
                            @Override
                            public void progress(long transferred, long total, float percentage) {
                            }
                        }));

                        float percentage = (float) (uploadParts.size() - uploadPart.number) / uploadParts.size();
                        //每上传成功一个分片更新进度信息
                        logger.info("asyncUploadFile --> running --> uploadPart :  total : {} , progress : {} %", uploadParts.size(), percentage * 100);

                        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objKey, result.getUploadId());
                        PartListing partListing = ossClient.listParts(listPartsRequest);
                        List<PartSummary> parts = partListing.getParts();
                        List<PartETag> partETags = new ArrayList<>();
                        for (int i = 0; i < parts.size(); i++) {
                            PartSummary part = parts.get(i);
                            PartETag partETag = new PartETag(i + 1, part.getETag());
                            partETags.add(partETag);
                        }
                        if (uploadParts.size() == partETags.size()) {
                            CompleteMultipartUploadRequest completeUploadRequest = new CompleteMultipartUploadRequest(
                                    bucketName, objKey,
                                    result.getUploadId(), partETags);
                            ossClient.completeMultipartUpload(completeUploadRequest);
                            logger.info("asyncUploadFile : end --> 【bucketName : {},objKey : {},file size : {}】",bucketName,objKey,file.length);
                        }
                    }

                } catch (Exception e) {
                    logger.error("分片上传异常", e);
                } finally {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            logger.error("关闭输入流异常",e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        } finally {
            if (ossClient != null) {
                ossClient.close();
            }
        }

    }

    @Async("AsyncTaskExecutor")
    public void asyncUploadFilePart(OSSClient ossClient, PartModel model) {
        ossService.uploadFileByPart(ossClient, model);
    }
}
