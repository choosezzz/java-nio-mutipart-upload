package com.tal.upload.web.param;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  18:16
 * @Description:
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilePartParam {

    @NotBlank(message = "数据集ID不能为空")
    private String id;
    @NotBlank(message = "数据子集名称不能为空")
    private String subsetName;
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;
    @NotNull(message = "分片大小不能为空")
    private long chunkSize;
    @NotNull(message = "文件总分块数量不能为空")
    private int totalSize;
    @NotNull(message = "文件块索引不能为空")
    private int partIndex;

    @JSONField(serialize = false)
    private MultipartFile chunk;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubsetName() {
        return subsetName;
    }

    public void setSubsetName(String subsetName) {
        this.subsetName = subsetName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }

    public MultipartFile getChunk() {
        return chunk;
    }

    public void setChunk(MultipartFile chunk) {
        this.chunk = chunk;
    }
}
