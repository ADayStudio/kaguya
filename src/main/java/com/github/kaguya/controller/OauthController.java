package com.github.kaguya.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.kaguya.config.SessionCookieContainer;
import com.github.kaguya.config.SystemProperty;
import com.github.kaguya.constant.OAuthType;
import com.github.kaguya.exception.model.ResponseMsg;
import com.github.kaguya.model.LocalOAuth;
import com.github.kaguya.model.LocalUser;
import com.github.kaguya.model.ThirdOAuth;
import com.github.kaguya.service.LocalOAuthService;
import com.github.kaguya.service.ThirdOAuthService;
import com.github.kaguya.service.LocalUserService;
import com.github.kaguya.util.SecurityUtil;
import com.xkcoding.justauth.AuthRequestFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OauthController {

    private final AuthRequestFactory factory;
    private static final String PREFIX = "/oauth";
    private static final int SUCCESS_CODE = 2000;

    @Resource
    private LocalUserService localUserService;
    @Resource
    private ThirdOAuthService thirdOAuthService;
    @Resource
    private LocalOAuthService localOAuthService;
    @Resource
    private SessionCookieContainer sessionCookieContainer;

    /**
     * 获取系统支持的所有登录类型
     */
    @GetMapping
    public Map<String, String> loginType() {
        List<String> oauthList = factory.oauthList();
        return oauthList.stream().collect(Collectors.toMap(
                oauth -> oauth.toLowerCase() + "登录",
                oauth -> SystemProperty.getHost() + "/oauth/login/" + oauth.toLowerCase()));
    }

    /**
     * 第三方登录
     */
    @RequestMapping(PREFIX + "/login/{oauthType}")
    public void renderAuth(@PathVariable String oauthType, HttpServletResponse response) throws IOException {
        AuthRequest authRequest = factory.get(getAuthSource(oauthType));
        response.sendRedirect(authRequest.authorize(oauthType + "::" + AuthStateUtils.createState()));
    }

    /**
     * 登录成功后的回调
     *
     * @param oauthType 第三方登录类型
     * @param callback  携带返回的信息
     */
    @RequestMapping(PREFIX + "/{oauthType}/callback")
    public ModelAndView login(@PathVariable("oauthType") String oauthType, AuthCallback callback,
                              HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView();

        AuthRequest authRequest = factory.get(getAuthSource(oauthType.toUpperCase()));
        // 可能会出现连接超时
        AuthResponse authResponse = null;
        try {
            authResponse = authRequest.login(callback);
            log.info("[response] = {}", JSONUtil.toJsonStr(response));
        }catch (Exception e){
            log.error("调用第三方验证失败", e);
        }


        // 获取第三方返回登录成功的信息
        AuthUser user = (AuthUser) authResponse.getData();
        String email = user.getEmail();
        if (SUCCESS_CODE != authResponse.getCode() || !SystemProperty.getEmail().equals(email)){
            return buildFailResult(modelAndView);
        }

        // 第三方数据
        String avatar = user.getAvatar();
        Long userId = Long.parseLong(user.getUuid());
        String token = user.getToken().getAccessToken();

        ThirdOAuth sessionUser = new ThirdOAuth();
        BeanUtils.copyProperties(user, sessionUser);
        sessionUser.setAvatar(SecurityUtil.base64(avatar.getBytes()));
        sessionUser.setUserId(userId);
        sessionUser.setToken(token);

        // 存储第三方登录用户信息
        thirdOAuthService.add(sessionUser);
        // session cookie
        String cookieValue = sessionCookieContainer.setSessionCookieValue(OAuthType.THIRD_TYPE, userId, token);
        sessionCookieContainer.setSessionCookie(request, response, cookieValue, 0);
        sessionCookieContainer.setSession(request, sessionUser, 0);
        modelAndView.setViewName("/index");
        return modelAndView;
    }

    /**
     * 验证登录类型
     */
    private AuthSource getAuthSource(String type) {
        if (StrUtil.isNotBlank(type)) {
            return AuthSource.valueOf(type.toUpperCase());
        } else {
            throw new RuntimeException("不支持的类型");
        }
    }

    /**
     * 认证失败处理
     */
    private ModelAndView buildFailResult(ModelAndView modelAndView) {
        modelAndView.addObject("result", ResponseMsg.buildFailResult("认证失败"));
        modelAndView.setViewName("/result");
        return modelAndView;
    }

    /**
     * 账号密码登录认证
     */
    @PostMapping(PREFIX + "/login")
    public ModelAndView login(@RequestParam(name = "email", required = false) String email,
                              @RequestParam(name = "password", required = false) String password,
                              HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView();

        if (StringUtils.isEmpty(email)){
            modelAndView.setViewName("/index");
            return modelAndView;
        }
        // 认证邮箱
        LocalUser localUser = localUserService.getUser(email);
        if (null == localUser) {
            return buildFailResult(modelAndView);
        }
        // 认证密码
        LocalOAuth auth = localOAuthService.getLocalOAuth(localUser.getUserId());
        if (null == auth) {
            return buildFailResult(modelAndView);
        }
        String inputPassword = sessionCookieContainer.getPassword(password, auth.getSalt());
        if (!inputPassword.equals(auth.getPassword())) {
            return buildFailResult(modelAndView);
        }

        // session cookie
        String cookieValue = sessionCookieContainer.setSessionCookieValue(auth);
        sessionCookieContainer.setSessionCookie(request, response, cookieValue, 0);
        sessionCookieContainer.setSession(request, localUser, 0);
        modelAndView.setViewName("/index");
        return modelAndView;
    }

}
