package com.example.demo.service;

import com.example.demo.dao.AuthContext;
import com.example.demo.utils.MapUtils;
import com.example.demo.utils.eql.Dql;
import org.apache.log4j.Logger;
import org.n3r.eql.EqlPage;
import org.n3r.eql.EqlTran;
import org.n3r.eql.util.Closes;
import org.n3r.idworker.Id;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.demo.utils.MapUtils.getInt;

/**
 * Created by beck on 2017/11/20.
 */
@Service
public class LeadService {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private DecimalFormat df = new DecimalFormat("#.00");
    private static Logger log=Logger.getLogger(LeadService.class);

    public List<Map> getAllLead(EqlPage page) {
        return new Dql().select("getAllLead").params(MapUtils.of("userId", AuthContext.getUserId())).limit(page).execute();
    }
    public List<Map> getOverDue(){
        return new Dql().select("getAllLead").params(MapUtils.of("userId", AuthContext.getUserId(),"overDue","1")).execute();
    }
    public List<Map> getLeadByCondition(Map map, EqlPage page) {
        map.put("userId",AuthContext.getUserId());
        return new Dql().select("getAllLead").params(map).limit(page).execute();
    }

    public Integer settle(String id) {
        Map map=new Dql().selectFirst("findLeadById").params(id,AuthContext.getUserId()).execute();
        return new Dql().update("settleLead").params(map).execute();
    }

    public Integer save(Map map) {
        List periodList = new ArrayList();
        long id = Id.next();
        map.put("id", id);
        map.put("userId",AuthContext.getUserId());
        if ("0".equals(MapUtils.getStr(map, "interestType"))) {
            int range = betweenMonth(MapUtils.getStr(map, "startTime"), MapUtils.getStr(map, "endTime"));
            String startTime = MapUtils.getStr(map, "startTime");
            for (int i = 0; i < range; i++) {
                String isLastNum = i == range - 1 ? "1" : "0";
                periodList.add(MapUtils.of("id", id, "num", i + 1, "numStartTime", relatveMonth(startTime, i), "numEndTime", relatveMonth(startTime, i + 1), "isLastNum", isLastNum, "totalNum", range));
            }
        } else if ("1".equals(MapUtils.getStr(map, "interestType"))) {
            periodList.add(MapUtils.of("id", id, "num", 1, "numStartTime", MapUtils.getStr(map, "startTime"), "numEndTime", MapUtils.getStr(map, "endTime"), "isLastNum", "1", "totalNum", "1"));
        }else if("2".equals(MapUtils.getStr(map,"interestType"))){
            int cycle = getInt(map, "cycle");
            map.put("startTime",MapUtils.getStr(map, "startTime"));
            map.put("endTime", relatveDay(MapUtils.getStr(map, "startTime"), cycle * 7));
            Date now=parse(MapUtils.getStr(map, "startTime"));
            for(int i=0;i<cycle;i++){
                String isLastNum = i == cycle - 1 ? "1" : "0";
                periodList.add(MapUtils.of("id", id, "num", i + 1, "numStartTime", relatveDay(now, i*7), "numEndTime", relatveDay(now, (i + 1)*7), "isLastNum", isLastNum, "totalNum", cycle));
            }
        }
        EqlTran eqlTran = new Dql().newTran();
        try {
            eqlTran.start();
            new Dql().insert("saveLeadUser").params(map).execute();
            new Dql().insert("insertPeriods").params(MapUtils.of("periodList", periodList)).execute();
            eqlTran.commit();
        } catch (Exception e) {
            e.printStackTrace();
            eqlTran.rollback();
            return 0;
        } finally {
            Closes.closeQuietly(eqlTran);
        }
        return 1;
    }

    public Map income() {
        List<Map> leadUserList = new Dql().select("getAllLeadWithValid").params(AuthContext.getUserId()).execute();
        Integer totalInvest = 0, mineTotalInvest = 0, totalInvestComed = 0, mineTotalInvestComed = 0;
        for (Map leadUser : leadUserList) {
            totalInvest += getInt(leadUser, "totalPrincipal");
            mineTotalInvest += getInt(leadUser, "principal");
            if ("0".equals(MapUtils.getStr(leadUser, "valid"))) {
                totalInvestComed += getInt(leadUser, "totalPrincipal");
                mineTotalInvestComed += getInt(leadUser, "principal");
            }
        }
        Map map = haveIncomed();
        Map needMap= MapUtils.of("totalInvest", totalInvest,
                "mineTotalInvest", mineTotalInvest,
                "totalInvestComed", totalInvestComed,
                "mineTotalInvestComed", mineTotalInvestComed,
                "totalIncome", map.get("totalIncome"),
                "monthIncome", monthIncome().get("currentMonthIncome"),
                "monthIncomed", monthIncome().get("currentMonthIncomeHaved"),
                "haveIncomed", map.get("haveIncomed"),
                "currentMonthIncome",currentMonth().get("currentMonthIncome"),
                "currentMonthIncomeHaved", currentMonth().get("currentMonthIncomeHaved")
                );
        needMap.putAll(queryCurrentMonthInvest());
        return needMap;
    }

    private Date parse(String str) {
        try {
            return sdf.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map haveIncomed() {
        List<Map> list = new Dql().select("getIncome").params(AuthContext.getUserId()).execute();
        double haveIncome = 0.0;
        double totalIncome = 0.0;
        for (Map map : list) {
            if ("1".equals(MapUtils.getStr(map, "interestType"))) {
                Date startTime = parse(MapUtils.getStr(map, "startTime"));
                Date endTime = parse(MapUtils.getStr(map, "endTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
            } else if("0".equals(MapUtils.getStr(map, "interestType"))){
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
            } else if("2".equals(MapUtils.getStr(map, "interestType"))){
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
            }
        }
        return MapUtils.of("totalIncome", totalIncome / 100, "haveIncomed", haveIncome / 100);
    }

    public Map monthIncome() {
        List<Map> periodList = new Dql().select("getIncome").params(AuthContext.getUserId()).execute();
        List list = new ArrayList();
        List list2=new ArrayList();
        for (int i = -9; i < 4; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + i);
            Map map=monthIncome(periodList,calendar.getTime());
            list.add(map.get("currentMonthIncome"));
            list2.add(map.get("currentMonthIncomeHaved"));
        }
        return MapUtils.of("currentMonthIncome",list,"currentMonthIncomeHaved",list2);
    }

    private Map currentMonth() {
        List<Map> list = new Dql().select("getIncome").params(AuthContext.getUserId()).execute();
        double haveIncome = 0.0;
        double totalIncome = 0.0;
        for (Map map : list) {
            Date startTime = parse(MapUtils.getStr(map, "startTime"));
            Date endTime = parse(MapUtils.getStr(map, "endTime"));
            Map firstEndMap = getFirstAndEndOfMonth(new Date());
            Date lastDayOfMonth = parse(firstEndMap.get("lastDayOfMonth").toString());
            Date firstDayOfMonth = parse(firstEndMap.get("firstDayOfMonth").toString());
            if (endTime.before(firstDayOfMonth) || endTime.after(lastDayOfMonth)) {
                continue;
            }
            if ("1".equals(MapUtils.getStr(map, "interestType"))) {
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
            } else if("0".equals(MapUtils.getStr(map, "interestType"))) {
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
            } else if("2".equals(MapUtils.getStr(map, "interestType"))){
                if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                    haveIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
                totalIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
            }
        }
        return MapUtils.of("currentMonthIncome", totalIncome / 100, "currentMonthIncomeHaved", haveIncome / 100);
    }

    private Map monthIncome(List<Map> list,Date date){
            double haveIncome = 0.0;
            double totalIncome = 0.0;
            for (Map map : list) {
                Date startTime = parse(MapUtils.getStr(map, "startTime"));
                Date endTime = parse(MapUtils.getStr(map, "endTime"));
                Map firstEndMap = getFirstAndEndOfMonth(date);
                Date lastDayOfMonth = parse(firstEndMap.get("lastDayOfMonth").toString());
                Date firstDayOfMonth = parse(firstEndMap.get("firstDayOfMonth").toString());
                if (endTime.before(firstDayOfMonth) || endTime.after(lastDayOfMonth)) {
                    continue;
                }
                if ("1".equals(MapUtils.getStr(map, "interestType"))) {
                    Long time = endTime.getTime() - startTime.getTime();
                    int range = (int) (time / 24 / 3600 / 1000);
                    if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                        haveIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
                    }
                    totalIncome += range * (double) map.get("interest") * (double) map.get("totalPrincipal");
                } else if("0".equals(MapUtils.getStr(map, "interestType"))) {
                    if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                        haveIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                    }
                    totalIncome += 1 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                } else if("2".equals(MapUtils.getStr(map, "interestType"))){
                    if ("1".equals(MapUtils.getStr(map, "payTag"))) {
                        haveIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                    }
                    totalIncome += 7 * (double) map.get("interest") * (double) map.get("totalPrincipal");
                }
            }
            return MapUtils.of("currentMonthIncome", totalIncome / 100, "currentMonthIncomeHaved", haveIncome / 100);
    }

    private Map getFirstAndEndOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayOfMonth = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date lastDayOfMonth = calendar.getTime();
        return MapUtils.of("firstDayOfMonth", sdf.format(firstDayOfMonth), "lastDayOfMonth", sdf.format(lastDayOfMonth));
    }


    private int betweenMonth(String startTimeString, String endTimeString) {
        Date endTime = parse(endTimeString);
        Date startTime = parse(startTimeString);
        int range = endTime.getMonth() - startTime.getMonth();
        int year = endTime.getYear() - startTime.getYear();
        if (range < 0) {
            range = range + 12 * year;
        }
        return range;
    }

    private String relatveMonth(String relatveMonth, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parse(relatveMonth));
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + offset);
        return sdf.format(calendar.getTime());
    }
    private String relatveDay(Date date,int offset){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + offset);
        return sdf.format(calendar.getTime());
    }

    private String relatveDay(String relatveDay, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parse(relatveDay));
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + offset);
        return sdf.format(calendar.getTime());
    }

    public List<Map> getPeriodById(Map param) {
        param.put("userId",AuthContext.getUserId());
        List<Map> periods=new Dql().select("findPeriodById").params(param).execute();
        periods.stream().forEach(map->{
            double totalPrincipal=(double)map.get("totalPrincipal");
            double interest= (double)map.get("interest");
            String isLastNum =MapUtils.getStr(map,"isLastNum");
            if("0".equals(MapUtils.getStr(map,"interestType"))){
                double income=totalPrincipal*interest/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",df.format(income));
            }else if("1".equals(MapUtils.getStr(map,"interestType"))){
                Date startTime = parse(MapUtils.getStr(map, "numStartTime"));
                Date endTime = parse(MapUtils.getStr(map, "numEndTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                double income=totalPrincipal*interest*range/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",df.format(income));
            }else if("2".equals(MapUtils.getStr(map,"interestType"))){
                int cycle = getInt(map, "cycle");
                double income=totalPrincipal*interest*7/100;
                income+=totalPrincipal/cycle;
                map.put("income",df.format(income));
            }
        });
        return periods;
    }

    public Integer updatePayNo(Map map) {
        Map period=new Dql().selectFirst("findLeadById").params(map.get("id"),AuthContext.getUserId()).execute();
        map.put("interestType",MapUtils.getStr(period,"interestType"));
        EqlTran tran=new Dql().newTran();
        try {
            tran.start();
            new Dql().useTran(tran).update("updatePayNum").params(map).execute();
            if("1".equals(map.get("type"))){
                new Dql().update("settleLead").params(map).execute();
            }
            tran.commit();
            return 1;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public List<Map> periodListInit(EqlPage page) {
        List<Map> leadList = new Dql().select("periodList").params(MapUtils.of("userId",AuthContext.getUserId())).limit(page).execute();
        List<Map> correctLeadList=addIncomeElm(leadList);
        log.debug("correctLeadList{}"+ correctLeadList);
        return correctLeadList;
    }

    public int getOverDueNum() {
        List<Map> overDueleadList = new Dql().select("periodList").params(MapUtils.of("overDue", "1","userId",AuthContext.getUserId())).execute();
        return overDueleadList.size();
    }

    public List<Map> getOverDue(Map param, EqlPage page) {
        param.put("userId",AuthContext.getUserId());
        List<Map> leadList = new Dql().select("periodList").params(param).limit(page).execute();
        List<Map> correctLeadList=addIncomeElm(leadList);
        log.debug("correctLeadList{}"+ correctLeadList);
        return correctLeadList;
    }

    private List<Map> addIncomeElm(List<Map> leadList){
        leadList.stream().forEach(map->{
            double totalPrincipal=(double)map.get("totalPrincipal");
            double interest= (double)map.get("interest");
            String isLastNum =MapUtils.getStr(map,"isLastNum");
            if("0".equals(MapUtils.getStr(map,"interestType"))){
                double income=totalPrincipal*interest/100;
                map.put("currentInterest",income);
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",df.format(income));
            }else if("1".equals(MapUtils.getStr(map,"interestType"))){
                Date startTime = parse(MapUtils.getStr(map, "numStartTime"));
                Date endTime = parse(MapUtils.getStr(map, "numEndTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                double income=totalPrincipal*interest*range/100;
                map.put("currentInterest",income);
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",df.format(income));
            }else if("2".equals(MapUtils.getStr(map,"interestType"))){
                int cycle = getInt(map, "cycle");
                double income=totalPrincipal*interest*7/100;
                map.put("currentInterest",income);
                income+=totalPrincipal/cycle;
                map.put("income",df.format(income));
            }
        });
        return leadList;
    }

    public int getToTimeNum() {
        List<Map> toTimeleadList = new Dql().select("periodList").params(MapUtils.of("toTime", "1","userId",AuthContext.getUserId())).execute();
        return toTimeleadList.size();
    }


    public Map queryCurrentMonthInvest() {
        Map map = getFirstAndEndOfMonth(new Date());
        map.put("userId",AuthContext.getUserId());
        int currentMonthInvestComeing = 0, currentMonthTotalInvestComeing = 0,//当月应收
                currentMonthInvestComed = 0, currentMonthTotalInvestComed = 0,//当月实收
                currentMonthInvest = 0, currentMonthTotalInvest = 0;//当月新增投资
        List<Map> list = new Dql().select("queryCurrentMonthInvestComed").params(map).execute(); //当月应收金额
        map.put("type", "1");
        List<Map> list2 = new Dql().select("queryCurrentMonthInvest").params(map).execute();//当月投资本金
        for (Map monthInvest : list) {
            if ("2".equals(MapUtils.getStr(monthInvest,"interestType"))){
                currentMonthInvestComeing += MapUtils.getInt(monthInvest, "principal")/MapUtils.getInt(monthInvest,"cycle");
                currentMonthTotalInvestComeing += MapUtils.getInt(monthInvest, "totalPrincipal")/MapUtils.getInt(monthInvest,"cycle");
                if (!"0".equals(MapUtils.getStr(monthInvest, "payTag"))) {
                    currentMonthInvestComed += MapUtils.getInt(monthInvest, "principal")/MapUtils.getInt(monthInvest,"cycle");
                    currentMonthTotalInvestComed += MapUtils.getInt(monthInvest, "totalPrincipal")/MapUtils.getInt(monthInvest,"cycle");
                }
            } else {
                if("1".equals(MapUtils.getStr(monthInvest,"isLastNum"))){
                    currentMonthInvestComeing += MapUtils.getInt(monthInvest, "principal");
                    currentMonthTotalInvestComeing += MapUtils.getInt(monthInvest, "totalPrincipal");
                }
                if("0".equals(MapUtils.getStr(monthInvest, "valid"))){
                    currentMonthInvestComed += MapUtils.getInt(monthInvest, "principal");
                    currentMonthTotalInvestComed += MapUtils.getInt(monthInvest, "totalPrincipal");
                }
            }
        }
        for (Map monthInvest : list2) {
            currentMonthInvest += MapUtils.getInt(monthInvest, "principal");
            currentMonthTotalInvest += MapUtils.getInt(monthInvest, "totalPrincipal");
        }
        return MapUtils.of("currentMonthInvestComeing",currentMonthInvestComeing,
                "currentMonthTotalInvestComeing",currentMonthTotalInvestComeing,
                "currentMonthInvestComed",currentMonthInvestComed,
                "currentMonthTotalInvestComed",currentMonthTotalInvestComed,
                "currentMonthInvest",currentMonthInvest,
                "currentMonthTotalInvest",currentMonthTotalInvest
                );
    }
}
