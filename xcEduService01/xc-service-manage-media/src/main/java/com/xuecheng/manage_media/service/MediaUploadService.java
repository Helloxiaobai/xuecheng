package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {

    @Autowired
    MediaFileRepository mediaFileRepository;

    @Value("${xc-service-manage-media.upload-location}")
    String upload_location;

    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    String routingkey_media_video;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     *根据文件md5等到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     *
     */
    //文件上传前的注册
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //1检查文件在磁盘上是否存在
        //文件所属目录的路径(整体的文件路径,到md5值,一个md5 标识一个文件以及所以分块文件)
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        //文件路径(最终要合并的文件路径以及全路径)
        String filePath = this.getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        boolean exists = file.exists();
        //检查文件信息是否在mongodb中存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if(optional.isPresent()&&exists){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //做准备工作 检查目录是否存在
        File fileFolder = new File(fileFolderPath);
        if(!fileFolder.exists()){
            fileFolder.mkdirs();
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //得到文件所属目录
    private String getFileFolderPath(String fileMd5){
        return upload_location + fileMd5.substring(0,1) +"/"+ fileMd5.substring(1,2) +"/"+ fileMd5 + "/";
    }
    private String getFilePath(String fileMd5,String fileExt){
        return upload_location + fileMd5.substring(0,1) +"/"+ fileMd5.substring(1,2) +"/"+ fileMd5  + "/"+ fileMd5 +"." + fileExt;
    }

    //检查分块
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //检查分块文件的是否存在
        String fileChunkFolderPath = this.getFileChunkFolderPath(fileMd5);
        File chunkFile = new File(fileChunkFolderPath + chunk);
        if(chunkFile.exists()){
            return new CheckChunkResult(CommonCode.SUCCESS,true);
        }else{
            return new CheckChunkResult(CommonCode.SUCCESS,false);
        }
    }

    private String getFileChunkFolderPath(String fileMd5){
        return upload_location + fileMd5.substring(0,1) +"/"+ fileMd5.substring(1,2) +"/"+ fileMd5 + "/chunk/";
    }

    public ResponseResult uploadchunk(MultipartFile file, String fileMd5, Integer chunk) {

        //检查分块目录 如果不存在则要自动创建
        //得到分块目录
        String fileChunkFolderPath = this.getFileChunkFolderPath(fileMd5);
        File chunckFileFolder = new File(fileChunkFolderPath);
        if(!chunckFileFolder.exists()){
            chunckFileFolder.mkdirs();
        }
        //得到上传文件的输入流
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = file.getInputStream();
            outputStream = new FileOutputStream(new File(fileChunkFolderPath + chunk));
            IOUtils.copy(inputStream,outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //合并所有分块
        //得到分块文件的所属目录
        String fileChunkFolderPath = this.getFileChunkFolderPath(fileMd5);
        File fileChunkFolder = new File(fileChunkFolderPath);
        if(!fileChunkFolder.exists()){
            fileChunkFolder.mkdirs();
        }
        //创建一个合并文件
        File file = new File(this.getFilePath(fileMd5, fileExt));
        //执行合并
        if(file.exists()){
            file.delete();
        }else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File[] files = fileChunkFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        File mergeFile = this.mergeFile(fileList, file);
        //校验文件的md5值是否和前端传入的md一致
        boolean md5Result = this.checkFileMd5(mergeFile, fileMd5);
        if(!md5Result){
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //将文件的信息写入到mongodb
        MediaFile newMediaFile = new MediaFile();
        newMediaFile.setFileId(fileMd5);
        newMediaFile.setFileName(fileMd5 + "." + fileExt);
        newMediaFile.setFileOriginalName(fileName);
        newMediaFile.setFilePath(fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) +"/"+fileMd5 +"/");
        newMediaFile.setFileSize(fileSize);
        newMediaFile.setMimeType(mimetype);
        newMediaFile.setFileType(fileExt);
        newMediaFile.setFileStatus("301002");
        MediaFile save = mediaFileRepository.save(newMediaFile);
        this.sendProcessVideoMsg(save.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //发送视频处理消息
    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.FAIL);
        }

        HashMap<String, String> map = new HashMap<>();
        map.put("mediaId",mediaId);
        String jsonString = JSON.toJSONString(map);
        //向mq发送消息
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_media_video,jsonString);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
    //执行合并
    private File mergeFile(List<File> chunkFileList,File mergeFile){
        try {
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
            byte[] b = new byte[1024];
        for (File chunkFile:chunkFileList) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "r");
            int len = -1;
            while ((len = raf_read.read(b))!=-1) {
                raf_write.write(b,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
        } catch (IOException e ) {
            e.printStackTrace();
            return null;
        }
        return mergeFile;
    }

    //校验文件
    private boolean checkFileMd5(File mergeFile,String md5){
        if(mergeFile==null || StringUtils.isEmpty(md5)){
            return false;
        }
        FileInputStream mergeFileInputStream = null;
        try {
            mergeFileInputStream = new FileInputStream(mergeFile);
            String md5Hex = DigestUtils.md5Hex(mergeFileInputStream);
            if(md5.equalsIgnoreCase(md5Hex)){
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                mergeFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}

