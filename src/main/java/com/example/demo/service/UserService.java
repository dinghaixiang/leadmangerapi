package com.example.demo.service;

import com.example.demo.dao.User;
import com.example.demo.event.UserRegisterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Created by beck on 2017/11/28.
 */
@Service
public class UserService {
    @Autowired
    ApplicationContext applicationContext;

    /**
     * 用户注册方法
     * @param user
     */
    public void register(User user) {
        //../省略其他逻辑

        //发布UserRegisterEvent事件
        applicationContext.publishEvent(new UserRegisterEvent(this,user));
    }
}
