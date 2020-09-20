package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRedis {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedis(){
        //定义key
        String key = "user_token:abde2951-test";
        HashMap<String, String> value = new HashMap<>();
        value.put("jwt","test111");
        value.put("regresh_token","test222");
        String jsonString = JSON.toJSONString(value);
        stringRedisTemplate.boundValueOps(key).set(jsonString,60, TimeUnit.SECONDS);
        String s = stringRedisTemplate.opsForValue().get(key);
        System.out.println(s);
    }
}
