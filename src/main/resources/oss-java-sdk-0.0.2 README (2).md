# TAL AI LAB OSS-JAVA-SDK

# About
oss-java-sdk is a object storage client that allows provisioning and control of a object storage . This includes support for bucket/object management, etc.

you can use the sdk on windows or ubuntu(16.04.9)  ,other platform we don't guarantee stability


# Start using 

You can obtain oss-java-sdk from our internal Maven Central using the following dependency:
```
<dependency>
    <groupId>com.tal.ailib</groupId>
    <artifactId>tal-java-sdk-oss</artifactId>
    <version>0.0.1</version>
</dependency>
```
## Configuration

### Using plain-old-Java
```
String accKey = "OFHRQRBF1IC07YDYIMFE";
String serKey = "TNNExvPDvQrxesXcVREWNhHjtVs9NOgHO6fkcQFW";
String endpoint = "221.122.128.3:80";

OSSClient client = new OSSClient(endpoint,accKey,serKey);

//do bussiness
.....


client.close();

```

### detail configuration

```
String accKey = "OFHRQRBF1IC07YDYIMFE";
String serKey = "TNNExvPDvQrxesXcVREWNhHjtVs9NOgHO6fkcQFW";
String endpoint = "221.122.128.3:80";

BasicOssCredentials credentials = new BasicOssCredentials(accKey, serKey);
ClientConfiguration conf = new ClientConfiguration();

conf.setConnectionTimeout(50 * 1000);
conf.setMaxConnections(50);
conf.setSocketTimeout(50 * 1000);

OSSClient client = new OSSClient(endpoint,credentials,conf);

//do bussiness
.....


client.close();


```

## Usage example
We support some types of operation includes **Bucket**, **Object**. 

### bucket API

#### upload by file path
```
String bucketName = "bucket02";
String objName = "video4.mp4";

PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objName, "E:\\video\\student\\video1.mp4");

ossClient.putObject(putObjectRequest);

```

#### upload by file 
```
String bucketName = "bucket02";
String objName = "video4.mp4";

File file = new File("E:\\video\\student\\video1.mp4");

PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objName, file);

ossClient.putObject(putObjectRequest);

```

#### upload by inputStream

```
String bucketName = "bucket02";
String objName = "video4.mp4";

File file = new File("E:\\video\\student\\video3.mp4");

ObjectMetadata objectMetadata = new ObjectMetadata();

//使用inputStream 最好指定Mimetype
objectMetadata.setContentType(Mimetypes.getInstance().getMimetype("file.mp4"));

client.putObject(new PutObjectRequest(bucketName, objName, new FileInputStream(file).withMetadata(objectMetadata));

```

#### callback ProgressListener

 - first create class implements ProgressListener

```
public class ProgressListenerDemo implements ProgressListener {
    @Override
    public void progress(long transferred, long total, float percentage) {
        System.out.println("transferred : "+transferred + " total : "+ total +" progress : " + percentage + " %");
    }
}

```
 - next call withProgressListener() function when create PutObjectRequest

```
String bucketName = "bucket02";
String objName = "video4.mp4";

File file = new File("E:\\video\\student\\video3.mp4");

client.putObject(new PutObjectRequest(bucketName, objName, new FileInputStream(file)).<PutObjectRequest>withProgressListener(new ProgressListenerDemo()));

```

#### piecewise upload (分片上传)

对于大文件上传，可以将其切分成片后上传。分片上传（Multipart Upload）主要适用于以下场景：

- 需要断点上传。
- 上传超过100MB大小的文件。
- 网络条件较差，和OSS的服务器之间的链接经常断开。
- 上传文件之前，无法确定上传文件的大小。

分片上传(Multipart Upload)分为如下4个步骤:

- 初始化一个分片上传事件（initiateMultipartUpload）。
- 逐个上传分片（uploadPart）。
- 查询已上传的分片列表（list multipart upload parts）。
- 完成分片上传（completeMultipartUpload）或取消分片上传(abortMultipartUpload)。



##### step 1 INITIATE MULTI-PART UPLOAD (初始化一个分片上传事件)

使用Multipart Upload模式传输数据前，必须先通知OSS初始化一个Multipart Upload事件。该操作会返回一个OSS服务器创建的全局唯一的Upload ID，用于标识本次Multipart Upload事件。用户可以根据这个ID来发起相关的操作，如中止Multipart Upload、查询Multipart Upload等。

调用ossClient.initiateMultipartUpload来初始化一个分片上传事件，示例代码如下：

```
String endpoint = "http://<your end point>";
String accessKeyId = "<yourAccessKeyId>";
String accessKeySecret = "<yourAccessKeySecret>";
String bucketName = "<yourBucketName>";
String key = "<yourKey>";

OSSClient ossClient = new OSSClient(endpoint,accessKeyId,accessKeySecret);
File file = new File("/home/upload/xxx.mp4");
if(!file.exists()){
    return;
}
//step 1
InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key));

```
提示：

- 用InitiateMultipartUploadRequest指定上传文件的名字和所属存储空间（bucketName）。
- 在InitiateMultipartUploadRequest中，您也可以设置ObjectMeta。
- initiateMultipartUpload 的返回结果中含有UploadId，它是区分分片上传事件的唯一标识，在后面的操作中将用到它。



##### step 2 MULTIPART UPLOAD PART(上传分片)

初始化一个Multipart Upload之后，可以根据指定的Object名称和Upload ID来分片上传数据，每个分片被称为一个Part。每一个上传的Part都有一个标识它的号码——分片号（part number，取值范围是1~10,000）。对于同一个Upload ID，分片号不但唯一标识这一块数据，也标识了这块数据在整个文件内的相对位置。如果用同一个分片号码上传了新的数据，那么OSS上已有的这个分片的数据将被覆盖。除了最后一块Part以外，其他的Part最小为5MB。最后一块Part没有大小限制。每个分片要严格按顺序上传。

调用ossClient.uploadPart上传分片的示例代码如下：
```
List partETags = new ArrayList<PartETag> ();
long partSize = 5 * 1024 * 1024;

ArrayList<IOUtils.UploadPart> uploadParts = IOUtils.splitFile(file.length(), partSize);

Collections.sort(uploadParts, new Comparator<IOUtils.UploadPart>() {

    @Override
    public int compare(IOUtils.UploadPart o1, IOUtils.UploadPart o2) {
        return o2.number - o1.number;
    }

});

for(IOUtils.UploadPart uploadPart : uploadParts){
    FileInputStream fileInputStream = new FileInputStream(file);
    fileInputStream.skip(uploadPart.offset);

    ossClient.uploadPart(new UploadPartRequest(conf.getBucketName(),conf.getObjName(),result.getUploadId(),uploadPart.number,fileInputStream,uploadPart.size).withProgressListener(new ProgressListener() {
        @Override
        public void progress(long transferred, long total, float percentage) {
            System.out.println("transferred : "+transferred + " total : "+ total +" progress : " + percentage + " %");
        }
    }));
}

```

##### step 3 LIST MULTIPART UPLOAD PARTS（获取已上传的分片）

获取上传的分片可以罗列出指定Upload ID所属的所有已经上传成功的分片。

调用ossClient.listParts获取某个上传事件所有已上传分片，代码示例如下
```
ListPartsRequest listPartsRequest = new ListPartsRequest(conf.getBucketName(),conf.getObjName(),result.getUploadId());
PartListing partListing = ossClient.listParts(listPartsRequest);
List<PartSummary> parts = partListing.getParts();
for(int i = 0;i < parts.size();i ++){
    PartSummary part = parts.get(i);
    // Part的ETag
    System.out.println(part.getETag());
    // Part的最后修改上传
    System.out.println(part.getLastModified());
    // 分片号，上传时候指定
    System.out.println(part.getPartNumber());
    // 分片数据大小
    System.out.println(part.getSize());

    PartETag partETag = new PartETag(i + 1, part.getETag());
    partETags.add(partETag);
}
```


##### step 4 COMPLETE MULTIPART UPLOAD（完成分片上传）

所有分片上传完成后，需要调用CompleteMultipartUpload接口来完成整个文件的分片上传。在执行该操作时，需要提供所有有效的partETags（包括PartNumber和ETag）。OSS收到提交的partETags后，会逐一验证每个Part的有效性。当所有的数据Part验证通过后，OSS将把这些Part组合成一个完整的Object。

调用ossClient.completeMultipartUpload完成分片上传的示例代码如下：
```
 CompleteMultipartUploadRequest completeUploadRequest = new CompleteMultipartUploadRequest(
        conf.getBucketName(), conf.getObjName(),
        result.getUploadId(), partETags);

CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeUploadRequest);

System.out.println(completeMultipartUploadResult.getBucketName());
System.out.println(completeMultipartUploadResult.getLocation());
System.out.println(completeMultipartUploadResult.getETag());
System.out.println(completeMultipartUploadResult.getBucketName());

System.out.println(ossClient.generateUrl(conf.getBucketName(),conf.getObjName(),(System.currentTimeMillis()/1000)+3600l).toString());

```

##### step 5 ABORT MULTIPART UPLOAD(取消分片上传事件)
您可以调用abortMultipartUpload接口，根据Upload ID中止对应的Multipart Upload事件。当一个Multipart Upload事件被中止后，就不能再使用这个Upload ID做任何操作，已经上传的Part数据也会被删除。


调用ossClient.abortMultipartUpload取消分片上传事件的示例代码如下：

```
AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(conf.getBucketName(), conf.getObjName(), result.getUploadId());
        ossClient.abortMultipartUpload(abortMultipartUploadRequest);
```

#### upload file(支持断点续传上传文件)
当上传大文件时，如果网络不稳定或者程序崩溃了，则整个上传就失败了。用户不得不重头再来，这样做不仅浪费资源，在网络不稳定的情况下，往往重试多次还是无法完成上传。OSS提供ossClient.uploadFile方法来实现断点续传上传

断点续传上传其实现的原理是，将要上传的文件分成若干个分片分别上传，所有分片都上传成功后，将所有分片合并成完整的文件，完成整个文件的上传。在上传的过程中会记录当前上传的进度信息（记录在checkpoint文件中），如果上传过程中某一分片上传失败，再次上传时会从checkpoint文件中记录的点继续上传。这要求再次调用时要指定与上次相同的checkpoint文件。上传完成后，checkpoint文件会被删除。

```
Conf conf = new Conf(".");
OSSClient ossClient = new OSSClient(conf.getEndpoint(),conf.getAccKey(),conf.getSerKey());
File file = new File(conf.getObjPath());
if(!file.exists()){
    return;
}

// 设置断点续传请求
UploadFileRequest uploadFileRequest = new UploadFileRequest(conf.getBucketName(), conf.getObjName());
// 指定上传的本地文件
uploadFileRequest.setUploadFile(conf.getObjPath());
// 指定上传的分片大小
uploadFileRequest.setPartSize(5 * 1024 * 1024);
// 开启断点续传
uploadFileRequest.setEnableCheckpoint(true);
// 断点续传上传
ossClient.uploadFile(uploadFileRequest.withProgressListener(new ProgressListener() {
    @Override
    public void progress(long transferred, long total, float percentage) {
        System.out.println("transferred : "+transferred + " total : "+ total +" progress : " + percentage + " %");
    }
}));
System.out.println(ossClient.generateUrl(conf.getBucketName(),conf.getObjName(),(System.currentTimeMillis()/1000)+3600l).toString());
// 关闭client
ossClient.close();

```
提示：

- 断点续传是分片上传的封装和加强，是用分片上传实现的。
- 文件较大或网络环境较差时，推荐使用分片上传。

### Object API

#### Generate Url

```
String bucketName = "bucket02";
String objName = "video4.mp4";

//过期时间必须是当前时间的秒数 + 未来过期时间，下例 设置过期时间为 从当前时刻起的 3600s

String url = client.generateUrl(bucketName, objName,  (System.currentTimeMillis() / 1000) + 3600l);
System.out.println("url : "+url);

```

That's all!  
any question you can email to suke1@100tal.com or contact suke at dingding







