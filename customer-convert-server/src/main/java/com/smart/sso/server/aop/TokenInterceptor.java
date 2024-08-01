package com.smart.sso.server.aop;


import com.smart.sso.client.rpc.SsoUser;
import com.smart.sso.server.common.ErrorCode;
import com.smart.sso.server.exception.BusinessException;
import com.smart.sso.server.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionManager sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SsoUser user = sessionManager.getUser(request);
        if (Objects.isNull(user)){
            log.error("用户没有登陆，检查token");
            String token = request.getHeader("token");// 从 http 请求头中取出 token
            if (null == token || "".equals(token)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object o, ModelAndView modelAndView) throws Exception {

    }
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object o, Exception e) throws Exception {
    }
}
