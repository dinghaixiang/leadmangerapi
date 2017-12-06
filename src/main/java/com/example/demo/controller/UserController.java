package com.example.demo.controller;

import com.example.demo.dao.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by beck on 2017/11/28.
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 注册控制方法
     * @param user 用户对象
     * @return
     */
    @RequestMapping(value = "/register")
    public String register(User user) {
        //调用注册业务逻辑
        userService.register(user);
        return "注册成功.";
    }
}
