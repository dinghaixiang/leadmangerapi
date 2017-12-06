package com.example.demo.web.handle;

import com.example.demo.dao.RspBean;
import com.example.demo.exception.TokenInvalidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by beck on 2017/11/28.
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Throwable.class)
    public RspBean handleThrowable(Throwable throwable) {
        throwable.printStackTrace();
        return new RspBean("1", "请求失败");
    }
    @ExceptionHandler(value = TokenInvalidException.class)
    public RspBean handleTokenInvalidException(TokenInvalidException tie) {
        return new RspBean("10", "登录信息已失效，请重新登录");
    }
}
