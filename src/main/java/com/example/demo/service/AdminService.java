package com.example.demo.service;

import com.example.demo.utils.MapUtils;
import com.example.demo.utils.eql.Dql;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by beck on 2017/12/1.
 */
@Service
public class AdminService {
    public Map findUserByName(Map map, HttpSession session){
       Map user= new Dql().selectFirst("findUserByName").params(map).execute();
        if(!MapUtils.isEmpty(user)){
            session.setAttribute("login",user);
        }
        return user;
    }
}
