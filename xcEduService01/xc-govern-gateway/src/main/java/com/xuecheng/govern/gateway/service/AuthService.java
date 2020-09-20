package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //从头取出jwt令牌
    public String getJwtFromHeader(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        if(StringUtils.isEmpty(authorization) || !authorization.startsWith("Bearer ")){
            return null;
        }
        return authorization.substring(7);
    }
    //从cookie取出token
    public String getTokenFromCookie(HttpServletRequest request){
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        String access_token = cookieMap.get("uid");
        if(StringUtils.isEmpty(access_token)){
            return null;
        }
        return access_token;
    }
    //从redis取出jwt令牌
    public long getExpire(String access_token){
        String key = "user_token:" + access_token;
        return stringRedisTemplate.getExpire(key);
    }
}
