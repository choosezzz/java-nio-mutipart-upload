package com.dearan.upload.service;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/30  14:24
 * @Description:
 */
@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    /**
     * 分片上传文件，通过随机读写方式，保存到本地临时目录下
     * @param fileName
     * @param chunkSize
     * @param partIndex
     * @param totalPart
     * @param chunkByte
     * @throws IOException
     */
    public void uploadFileRandomAccessFile(String fileName,long chunkSize,int partIndex,int totalPart,byte[] chunkByte){

        String tempDirPath ="C:/tmp2";
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(tempDirPath);
        File tmpFile = new File(tempDirPath, tempFileName);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        try {
            RandomAccessFile accessTmpFile = new RandomAccessFile(tmpFile, "rw");
            long offset = chunkSize * partIndex;
            //定位到该分片的偏移量
            accessTmpFile.seek(offset);
            //写入该分片数据
            accessTmpFile.write(chunkByte);
            // 释放
            accessTmpFile.close();
            boolean isOk = checkUploadProgress(fileName,totalPart,partIndex,tempDirPath);
            if (isOk) {
                renameFile(tmpFile, fileName);
                logger.info("分片上传文件完成！！-->【fileName : {}】",fileName);
            }
        }catch (Exception e){
            logger.error("上传发生异常",e);
        }
    }

    /**
     * 通过Java NIO的MappedByteBuffer上传文件
     * @param fileName
     * @param chunkSize
     * @param partIndex
     * @param totalPart
     * @param chunkByte
     * @throws IOException
     */
    public void uploadFileByMappedByteBuffer(String fileName,long chunkSize,int partIndex,int totalPart,byte[] chunkByte) {

        String uploadDirPath = "C:/tmp1";
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(uploadDirPath);
        File tmpFile = new File(uploadDirPath, tempFileName);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
       try {
           RandomAccessFile tempRaf = new RandomAccessFile(tmpFile, "rw");
           FileChannel fileChannel = tempRaf.getChannel();
           //写入该分片数据
           long offset = chunkSize * partIndex;
           MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, offset, chunkByte.length);
           mappedByteBuffer.put(chunkByte);
           // 释放
           this.freedMappedByteBuffer(mappedByteBuffer);
           fileChannel.close();
           boolean isOk = checkUploadProgress(fileName,totalPart,partIndex, uploadDirPath);
           if (isOk) {
               renameFile(tmpFile, fileName);
               logger.info("文件上传完成--> 【fileName：{}】",fileName);
           }
       }catch (Exception e){
            logger.error("上传发生异常",e);
       }
    }

    /**
     * 检查文件是上传完成
     * @param fileName
     * @param totalPart
     * @param partIndex
     * @param uploadDirPath
     * @return
     * @throws IOException
     */
    private boolean checkUploadProgress(String fileName,int totalPart,int partIndex, String uploadDirPath) throws IOException {

        File confFile = new File(uploadDirPath, fileName + ".conf");
        RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");

        //把该分段标记为 true 表示完成
        accessConfFile.setLength(totalPart);
        accessConfFile.seek(partIndex);
        accessConfFile.write(Byte.MAX_VALUE);

        //completeList 检查是否全部完成,如果数组里是否全部都是(全部分片都成功上传)
        byte[] completeList = FileUtils.readFileToByteArray(confFile);
        byte isComplete = Byte.MAX_VALUE;
        for (int i = 0; i < completeList.length && isComplete == Byte.MAX_VALUE; i++) {
            //与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE
            isComplete = (byte) (isComplete & completeList[i]);
        }
        logger.info("检查文件是否上传完成--->【fileName : {} ,result : {}】",fileName,isComplete);
        accessConfFile.close();
        if (isComplete == Byte.MAX_VALUE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 重命名文件
     * @param toBeRenamed
     * @param toFileNewName
     * @return
     */
    private boolean renameFile(File toBeRenamed, String toFileNewName) {
        //检查要重命名的文件是否存在，是否是文件
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            logger.info("File does not exist: {}" , toBeRenamed.getName());
            return false;
        }
        String p = toBeRenamed.getParent();
        File newFile = new File(p + File.separatorChar + toFileNewName);
        //修改文件名
        return toBeRenamed.renameTo(newFile);
    }

    /**
     * 在MappedByteBuffer释放后再对它进行读操作的话就会引发jvm crash，在并发情况下很容易发生
     * 正在释放时另一个线程正开始读取，于是crash就发生了。所以为了系统稳定性释放前一般需要检 查是否还有线程在读或写
     * @param mappedByteBuffer
     */
    private void freedMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }

            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedByteBuffer,
                                new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        logger.error("clean MappedByteBuffer error!!!", e);
                    }
                    logger.info("clean MappedByteBuffer completed!!!");
                    return null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
