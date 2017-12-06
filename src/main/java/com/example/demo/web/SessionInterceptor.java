package com.example.demo.web;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.example.demo.exception.TokenInvalidException;
import com.example.demo.utils.MapUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by beck on 2017/11/27.
 */
@Component
public class SessionInterceptor implements HandlerInterceptor {
    private static Log log= LogFactory.getLog(SessionInterceptor.class);
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        log.debug("interceptor come in......");
        log.debug("httpServletRequest.getServletPath()"+httpServletRequest.getServletPath());
//        if(httpServletRequest.getServletPath().indexOf("/login")!=-1){
//            return true;
//        }
        if(MapUtils.isEmpty((Map) httpServletRequest.getSession().getAttribute("login"))){
            log.debug("拦住了");
            throw new TokenInvalidException();
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
