package com.smart.sso.server.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.session.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.smart.sso.client.constant.Oauth2Constant;
import com.smart.sso.client.rpc.SsoUser;
import com.smart.sso.server.service.UserService;
import com.smart.sso.server.session.CodeManager;
import com.smart.sso.server.session.SessionManager;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户和登录管理
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private CodeManager codeManager;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private UserService userService;


    /**
     * 登录提交
     *
     * @param username
     * @param password
     * @param request
     * @param response
     * @return
     * @throws UnsupportedEncodingException
     */
    @PostMapping("/login")
    public BaseResponse<SsoUser> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request, HttpServletResponse response) {
        SsoUser user = userService.login(username, password);
        String tgt = sessionManager.setUser(user, request, response);
//        generateCodeAndRedirect("", tgt);
        return ResultUtils.success(user);
    }

    @PostMapping("/signup")
    public BaseResponse<SsoUser> signup(@RequestParam String username, @RequestParam String password, @RequestParam String roleName) {
        String userId = userService.signup(username, password, roleName);
        return ResultUtils.success(userService.getUserById(userId));
    }

    @PostMapping("/role")
    public BaseResponse<Role> role(@RequestParam String role, @RequestParam String roleName, @RequestParam String permission) {
        Role result = userService.signRole(role, roleName, permission);
        return ResultUtils.success(result);
    }

//    @RequestMapping(method = RequestMethod.GET)
    public BaseResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionManager.invalidate(request, response);
        return ResultUtils.success(null);
    }

    /**
     * 生成授权码，跳转到redirectUri
     *
     * @param redirectUri
     * @param tgt
     * @return
     * @throws UnsupportedEncodingException
     */
    private String generateCodeAndRedirect(String redirectUri, String tgt) throws UnsupportedEncodingException {
        // 生成授权码
        String code = codeManager.generate(tgt, true, redirectUri);
        return "redirect:" + authRedirectUri(redirectUri, code);
    }

    /**
     * 将授权码拼接到回调redirectUri中
     *
     * @param redirectUri
     * @param code
     * @return
     * @throws UnsupportedEncodingException
     */
    private String authRedirectUri(String redirectUri, String code) throws UnsupportedEncodingException {
        StringBuilder sbf = new StringBuilder(redirectUri);
        if (redirectUri.indexOf("?") > -1) {
            sbf.append("&");
        } else {
            sbf.append("?");
        }
        sbf.append(Oauth2Constant.AUTH_CODE).append("=").append(code);
        return URLDecoder.decode(sbf.toString(), "utf-8");
    }

}