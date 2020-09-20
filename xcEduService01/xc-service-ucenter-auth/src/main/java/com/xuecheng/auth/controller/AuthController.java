package com.xuecheng.auth.controller;

import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController implements AuthControllerApi {
    @Value("${auth.clientId}")
    String clientId;

    @Value("${auth.cookieDomain}")
    String cookieDomain;
    @Value("${auth.clientSecret}")
    String clientSecret;
    @Value("${auth.cookieMaxAge}")
    int cookieMaxAge;
    @Autowired
    AuthService authService;

    @Override
    @PostMapping("/userlogin")
    public LoginResult login(LoginRequest loginRequest) {
        if(loginRequest == null || StringUtils.isEmpty(loginRequest.getUsername())){
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        if(StringUtils.isEmpty(loginRequest.getPassword())){
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        AuthToken authToken = authService.login(username,password,clientId,clientSecret);
        //将令牌存储到cookie
        String jwt_token = authToken.getJwt_token();
        saveCookie(jwt_token);
        return new LoginResult(CommonCode.SUCCESS,jwt_token);
    }

    private void saveCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response,cookieDomain,"/","uid",token,cookieMaxAge,false);
    }

    @Override
    @PostMapping("/userlogout")
    public ResponseResult logout() {
        //删除redis中的token
        String token = this.getTokenFormCookie();
        boolean result = authService.deleteToken(token);
        //清除cookie
        this.clearCookie(token);
        return new ResponseResult(CommonCode.SUCCESS);
    }
    //从cookie删除token
    private void clearCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response,cookieDomain,"/","uid",token,0,false);
    }

    @Override
    @GetMapping("/userjwt")
    public JwtResult userjwt() {
        //取出cookie
        String uid = this.getTokenFormCookie();
        if(uid == null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        //拿着身份令牌从redis 中查询jwt令牌
        AuthToken userToken = authService.getUserToken(uid);
        //将jwt令牌返回给用户
        if(userToken != null){
            String access_token = userToken.getAccess_token();
            return new JwtResult(CommonCode.SUCCESS,access_token);
        }
        return null;

    }
    //取出cookie中的身份令牌
    private String getTokenFormCookie(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> map = CookieUtil.readCookie(request, "uid");
        if(map.get("uid") != null){
            String uid = map.get("uid");
            return uid;
        }
        return null;
    }
}
