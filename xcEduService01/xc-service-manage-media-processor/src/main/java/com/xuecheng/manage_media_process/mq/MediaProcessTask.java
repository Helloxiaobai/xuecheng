package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;

    @Value("${xc-service-manage-media.video-location}")
    String serverPath;

    @Autowired
    MediaFileRepository mediaFileRepository;
    //接受视频处理消息进行视频处理
    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}")
    private void receiveMediaProcessTask(String msg){
        //1 解析消息内容 得到mediaid
        Map map = JSON.parseObject(msg, Map.class);
        String mediaId = (String)map.get("mediaId");
        //2 拿mediaId从数据库查询文件信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            return;
        }
        MediaFile mediaFile = optional.get();
            //判断文件类型
        String fileType = mediaFile.getFileType();
        if(!"avi".equals(fileType)){
            mediaFile.setProcessStatus("303004");//无需处理
            mediaFileRepository.save(mediaFile);
            return;
        }else{
            //需要处理
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        //3 使用工具类将avi文件生成mp4
        //要处理的视频文件的路径
        String videoPath = serverPath + mediaFile.getFilePath() + mediaFile.getFileName();
        //生成mp4的文件名称
        String mp4_name = mediaFile.getFileId() + ".mp4";
        //生成mp4所在的路径
        String mp4folder_path = serverPath + mediaFile.getFilePath();

        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpeg_path, videoPath, mp4_name, mp4folder_path);
        String result = mp4VideoUtil.generateMp4();
        if(result == null||!"success".equals(result)){
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //4 将mp4生成m3u8和ts文件
        String mp4_video_path = serverPath + mediaFile.getFilePath() + mp4_name;
        //m3u8_name 文件名称
        String m3u8_name = mediaFile.getFileId() + ".m3u8";
        //m3u8文件所在目录
        String m3u8folder_path = serverPath + mediaFile.getFilePath() + "hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpeg_path,mp4_video_path,m3u8_name,m3u8folder_path);
        String tsResult = hlsVideoUtil.generateM3u8();
        if(!"success".equals(tsResult)){
            //处理失败
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(tsResult);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        mediaFile.setProcessStatus("303002");
        //保存fileUrl(此url就是视频播放的相对路径)
        String fileUrl = mediaFile.getFilePath() + "hls/" + m3u8_name;
        mediaFile.setFileUrl(fileUrl);
        mediaFileRepository.save(mediaFile);
    }
}
