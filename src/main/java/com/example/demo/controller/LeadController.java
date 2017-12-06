package com.example.demo.controller;

import com.example.demo.dao.RspBean;
import com.example.demo.service.LeadService;
import com.example.demo.utils.MapUtils;
import org.n3r.eql.EqlPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by beck on 2017/11/20.
 */
@RestController
@RequestMapping("/lead")
public class LeadController {
    @Autowired
    private LeadService service;
    @RequestMapping(value = "/init",method = RequestMethod.POST)
    public RspBean init(){
        EqlPage page = new EqlPage(0, 6);
        return new RspBean(MapUtils.of("leadUser",service.getAllLead(page),"eqlPage",page));
    }
    @RequestMapping(value = "/list",method = RequestMethod.POST)
    public RspBean query(@RequestBody Map map){
        EqlPage page=new EqlPage(MapUtils.getInt(map,"startIndex"),MapUtils.getInt(map,"pageRows"));
        return new RspBean(MapUtils.of("leadUser",service.getLeadByCondition(map,page),"eqlPage",page));
    }
    @RequestMapping(value = "/settle",method = RequestMethod.POST)
    public RspBean settle(@RequestBody Map map){
        return new RspBean(service.settle(map.get("id").toString()));
    }

    @RequestMapping(value="/save",method = RequestMethod.POST)
    public RspBean save(@RequestBody Map param){
        return new RspBean(service.save(param));
    }
    @RequestMapping(value="/income",method = RequestMethod.POST)
    public RspBean income(){
        return new RspBean(service.income());
    }
    @RequestMapping(value="/month-income",method = RequestMethod.POST)
    public RspBean monthIncome(){
        return new RspBean(service.monthIncome());
    }

    @RequestMapping(value="/get-periods",method = RequestMethod.POST)
    public RspBean period(@RequestBody Map map){
        return new RspBean(MapUtils.of("periodList",service.getPeriodById(map)));
    }
    @RequestMapping(value="/update-pay-no",method = RequestMethod.POST)
    public RspBean updatePayNo(@RequestBody Map map){
        return new RspBean(service.updatePayNo(map));
    }
    @RequestMapping(value="/period-init",method = RequestMethod.POST)
    public RspBean periodListInit(){
        EqlPage page = new EqlPage(0, 6);
        return new RspBean(MapUtils.of("periodList",service.periodListInit(page),"eqlPage",page,"overDueNum",service.getOverDueNum(),"toTimeNum",service.getToTimeNum()));
    }

    @RequestMapping(value="/period-list",method = RequestMethod.POST)
    public RspBean periodList(@RequestBody Map map){
        EqlPage page=new EqlPage(MapUtils.getInt(map,"startIndex"),MapUtils.getInt(map,"pageRows"));
        return new RspBean(MapUtils.of("periodList",service.getOverDue(map,page),"eqlPage",page));
    }
}
