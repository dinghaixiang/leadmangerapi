package com.example.demo.service;

import com.example.demo.dao.AuthContext;
import com.example.demo.utils.MapUtils;
import com.example.demo.utils.eql.Dql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by beck on 2017/12/27.
 */
@Component
public class ScheduleTaskService {
    @Autowired
    private LeadService service;

    @Scheduled(cron = "0 0 0 * * ?")//每天凌晨0:00执行任务
    public void auto() {
        System.out.print("ScheduleTask=============start");
        AuthContext.setUserId("3");
        Map map=service.income();
        map.put("netInvestmentTotal", MapUtils.getInt(map,"currentMonthTotalInvest")-MapUtils.getInt(map,"currentMonthTotalInvestComed"));
        map.put("netInvestment", MapUtils.getInt(map,"currentMonthInvest")-MapUtils.getInt(map,"currentMonthInvestComed"));
        new Dql().insert("insertStatics").params(map).execute();
        new Dql().insert("insertInvestStatics").params(map).execute();
        System.out.print("ScheduleTask=============end");
    }
}
