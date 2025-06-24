package com.hmall.common.interceptor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class UserInfoInterceptor implements HandlerInterceptor{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求头user-info
        String userId = request.getHeader("user-info");
        //如果有值的话设置到线程副本ThreadLocal中
        if(StringUtils.isNotBlank(userId)){
            UserContext.setUser(Long.parseLong(userId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求执行完成后要清除ThreadLocal中的信息
        UserContext.removeUser();
    }
}
