package com.example.demo.event;

import com.example.demo.dao.User;
import lombok.Data;
import org.springframework.context.ApplicationEvent;
@Data
public class UserRegisterEvent extends ApplicationEvent {
    private User user;
    public UserRegisterEvent(Object source,User user){
        super(source);
        this.user=user;
    }
}
