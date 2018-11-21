package com.dearan.upload.service;

import client.OSSClient;
import com.dearan.upload.model.PartModel;
import com.dearan.upload.model.UploadModel;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:38
 * @Description:
 */
public interface OssService {

    boolean uploadMinFile(UploadModel model);
    boolean uploadFileByPart(OSSClient ossClient,PartModel model);
}
