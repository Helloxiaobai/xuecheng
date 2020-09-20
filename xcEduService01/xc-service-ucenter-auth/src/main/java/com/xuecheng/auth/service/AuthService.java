package com.xuecheng.auth.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.exception.ExceptionCast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //用户认证申请令牌 将令牌存储到redis
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        AuthToken authToken = this.applyToken(username, password, clientId, clientSecret);
        if(authToken == null){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }
        //将令牌存储到redis
        String jsonString = JSON.toJSONString(authToken);
        boolean token = saveToken(authToken.getJwt_token(), jsonString, tokenValiditySeconds);
        if(!token){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }

    //申请令牌
    private AuthToken applyToken(String username, String password, String clientId, String clientSecret){
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        URI uri = serviceInstance.getUri();
        String authUrl = uri + "/auth/oauth/token";
        //定义header
        LinkedMultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        String httpBasic = getHttpBasic(clientId, clientSecret);
        header.add("Authorization",httpBasic);
        //定义body
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type","password");
        body.add("username",username);
        body.add("password",password);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, header);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if(response.getRawStatusCode()!=400&&response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, httpEntity, Map.class);
        Map bodyMap = exchange.getBody();
        if(bodyMap == null ||
                bodyMap.get("access_token") == null ||
                bodyMap.get("refresh_token")==null ||
                bodyMap.get("jti")== null){
            //解析返回信息
            if(bodyMap != null && bodyMap.get("error_description") != null){
                String error_description = (String)bodyMap.get("error_description");
                if(error_description.contains("UserDetailsService returned null")){
                    ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
                }else if(error_description.contains("坏的凭证")){
                    ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
                }
            }
            return null;
        }
        String access_token = (String)bodyMap.get("access_token");
        String refresh_token = (String)bodyMap.get("refresh_token");
        String jwt_token = (String)bodyMap.get("jti");
        return new AuthToken(access_token,refresh_token,jwt_token);
    }
    private String getHttpBasic(String clientId,String clientSecret){
        String string = clientId + ":" + clientSecret;
        byte[] encode = Base64Utils.encode(string.getBytes());
        return "Basic "+ new String(encode);
    }
    //存储令牌到redis

    private boolean saveToken(String jti,String conetent,long ttl){
        String key = "user_token:" + jti;
        stringRedisTemplate.boundValueOps(key).set(conetent,ttl, TimeUnit.SECONDS);
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire > 0;
    }


    //从redis查询令牌
    public AuthToken getUserToken(String token){
        String key = "user_token:" + token;
        String value = stringRedisTemplate.opsForValue().get(key);
        //转成对象
        AuthToken authToken = null;
        try {
            authToken = JSON.parseObject(value, AuthToken.class);
            return authToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //从redis中删除token
    public boolean deleteToken(String jti){
        String key ="user_token:" + jti;
        stringRedisTemplate.delete(key);
        stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return true;
    }
}
