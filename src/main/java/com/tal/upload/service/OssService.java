package com.tal.upload.service;

import client.OSSClient;
import com.tal.upload.model.PartModel;
import com.tal.upload.model.UploadModel;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:38
 * @Description:
 */
public interface OssService {

    boolean uploadMinFile(UploadModel model);
    boolean uploadFileByPart(OSSClient ossClient,PartModel model);
}
