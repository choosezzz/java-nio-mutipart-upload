package com.tal.upload.redis;

import com.tal.axer.middleware.redis.RedisClientTemplate;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: dingshuangen
 * @Date: 2018/10/26  9:30
 * @Description:
 */
@Log4j2
@Service
public class UploadRedisService {

    @Autowired
    private RedisClientTemplate redisClientTemplate;
    private static final String UPLOAD_TASK_PART_PRE="upload_task_part_list";

    public void setPartId(String uploadId,int partId){
        log.info("setPartId :start --> 【uploadId :{} ,partId :{}】 ",uploadId,partId);
        String key = String.format("%s_%s",UPLOAD_TASK_PART_PRE,uploadId);
        redisClientTemplate.lpush(key,String.valueOf(partId));
        log.info("setPartId :end --> 【key :{} ,partId :{}】 ",key,partId);
    }

    public Integer getPartId(String uploadId){
        log.info("getPartId :start --> 【uploadId :{}】 ",uploadId);
        String key = String.format("%s_%s",UPLOAD_TASK_PART_PRE,uploadId);
        String result = redisClientTemplate.lindex(key, 0);
        if (null == result){
            log.info("setPartId :end, result is null --> 【key :{}】 ",key);
            return null;
        }else {
            log.info("setPartId :end --> 【key :{} ,result :{}】 ",key,result);
            return Integer.parseInt(result);
        }
    }

    public long deleteUploadTask(String uploadId){
        log.info("deleteUploadTask :start --> 【uploadId :{} 】 ",uploadId);
        String key = String.format("%s_%s",UPLOAD_TASK_PART_PRE,uploadId);
        Long result = redisClientTemplate.del(key);
        log.info("deleteUploadTask :end --> 【key :{} ,result :{}】 ",key,result);
        return result;
    }
}
