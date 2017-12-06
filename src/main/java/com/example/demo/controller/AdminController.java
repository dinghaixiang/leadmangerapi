package com.example.demo.controller;

import com.example.demo.dao.RspBean;
import com.example.demo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Created by beck on 2017/12/1.
 */
@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public RspBean login(@RequestBody Map map, HttpSession session){
       return new RspBean(adminService.findUserByName(map,session));
    }
}
