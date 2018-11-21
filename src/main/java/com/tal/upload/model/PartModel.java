package com.tal.upload.model;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:46
 * @Description:
 */
public class PartModel {

    byte[] filePart;
    String uploadId;
    int partId;
    int totalPart;
    private String bucketName;
    private String objName;

    private boolean last;

    public int getTotalPart() {
        return totalPart;
    }

    public void setTotalPart(int totalPart) {
        this.totalPart = totalPart;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjName() {
        return objName;
    }

    public void setObjName(String objName) {
        this.objName = objName;
    }

    public byte[] getFilePart() {
        return filePart;
    }

    public void setFilePart(byte[] filePart) {
        this.filePart = filePart;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

}
