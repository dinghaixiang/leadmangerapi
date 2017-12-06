package com.example.demo.listener;

import com.example.demo.dao.User;
import com.example.demo.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created by beck on 2017/11/28.
 */
@Component
public class AnnotationRegisterListener {
    @EventListener
    public void register(UserRegisterEvent userRegisterEvent) {
        //获取注册用户对象
        User user = userRegisterEvent.getUser();

        //../省略逻辑

        //输出注册用户信息
        System.out.println("@EventListener注册信息，用户名："+user.getName()+"，密码："+user.getPassword());
    }
}
