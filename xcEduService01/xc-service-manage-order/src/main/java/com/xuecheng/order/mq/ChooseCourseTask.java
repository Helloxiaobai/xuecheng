package com.xuecheng.order.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

@Component
public class ChooseCourseTask {

    @Autowired
    TaskService taskService;

    @Autowired
    RabbitTemplate rabbitTemplate;


    private static final Logger LOGGER = LoggerFactory.getLogger(ChooseCourseTask.class);

//    @Scheduled(cron = "0/10 * * * * *")
//    public void task1(){
//        LOGGER.info("=============测试定时任务开始1开始==============");
//        try{
//            Thread.sleep(1000);
//        }catch(InterruptedException e){
//            e.printStackTrace();
//        }
//        LOGGER.info("=============测试定时任务1结束===================");
//    }

    @Scheduled(cron = "0/10 * * * * *")
    public void sendChoosecourseTask(){
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.set(GregorianCalendar.MINUTE,-1);
        Date time = calendar.getTime();
        List<XcTask> xcTaskList = taskService.findXcTaskList(time, 100);
        System.out.println(xcTaskList);
        //调用service发布消息  将添加选课的任务发送给mq
        for (XcTask xcTaskItem:xcTaskList) {
            //取任务
            if(taskService.getTask(xcTaskItem.getId(),xcTaskItem.getVersion()) > 0){
                String ex = xcTaskItem.getMqExchange();//要发送的交换机
                String routingKey = xcTaskItem.getMqRoutingkey();//发消息要带routingKey
                taskService.publish(xcTaskItem,ex,routingKey);
            }
        }
    }




    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE)
    public void reciveFinishChooseCourse(XcTask xcTask){
        if(StringUtils.isNotEmpty(xcTask.getId()))
        taskService.finishTask(xcTask.getId());
    }
}
