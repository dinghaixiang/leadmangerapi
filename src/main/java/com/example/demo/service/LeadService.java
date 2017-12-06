package com.example.demo.service;

import com.example.demo.utils.MapUtils;
import com.example.demo.utils.eql.Dql;
import org.n3r.eql.EqlPage;
import org.n3r.eql.EqlTran;
import org.n3r.eql.util.Closes;
import org.n3r.idworker.Id;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by beck on 2017/11/20.
 */
@Service
public class LeadService {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public List<Map> getAllLead(EqlPage page) {
        return new Dql().select("getAllLead").limit(page).execute();
    }

    public List<Map> getLeadByCondition(Map map, EqlPage page) {
        return new Dql().select("getAllLead").params(map).limit(page).execute();
    }

    public Integer settle(String id) {
        Map map=new Dql().selectFirst("findLeadById").params(id).execute();
        return new Dql().select("settleLead").params(map).execute();
    }

    public Integer save(Map map) {
        List periodList = new ArrayList();
        long id = Id.next();
        map.put("id", id);
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
            int cycle = MapUtils.getInt(map,"cycle");
            Date now=new Date();
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
        List<Map> leadUserList = new Dql().select("getAllLeadWithValid").execute();
        Integer totalInvest = 0, mineTotalInvest = 0, totalInvestComed = 0, mineTotalInvestComed = 0;
        for (Map leadUser : leadUserList) {
            totalInvest += MapUtils.getInt(leadUser, "totalPrincipal");
            mineTotalInvest += MapUtils.getInt(leadUser, "principal");
            if ("0".equals(MapUtils.getStr(leadUser, "valid"))) {
                totalInvestComed += MapUtils.getInt(leadUser, "totalPrincipal");
                mineTotalInvestComed += MapUtils.getInt(leadUser, "principal");
            }
        }
        Map map = haveIncomed();
        return MapUtils.of("totalInvest", totalInvest,
                "mineTotalInvest", mineTotalInvest,
                "totalInvestComed", totalInvestComed,
                "mineTotalInvestComed", mineTotalInvestComed,
                "totalIncome", map.get("totalIncome"),
                "monthIncome", monthIncome().get("currentMonthIncome"),
                "monthIncomed", monthIncome().get("currentMonthIncomeHaved"),
                "haveIncomed", map.get("haveIncomed"),
                "currentMonthIncome",currentMonth().get("currentMonthIncome"),
                "currentMonthIncomeHaved",currentMonth().get("currentMonthIncomeHaved")
                );
    }

    private Date parse(String str) {
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map haveIncomed() {
        List<Map> list = new Dql().select("getIncome").execute();
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
        List<Map> periodList = new Dql().select("getIncome").execute();
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
        List<Map> list = new Dql().select("getIncome").execute();
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
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayOfMonth = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date lastDayOfMonth = calendar.getTime();
        return MapUtils.of("firstDayOfMonth", sdf.format(firstDayOfMonth), "lastDayOfMonth", sdf.format(lastDayOfMonth));
    }

    public double computeMonthIncome(List<Map> list) {
        double monthIncome = 0.0;
        for (Map leadUser : list) {
            double interest = Double.valueOf(MapUtils.getStr(leadUser, "interest"));
            double totalPrincipal = Double.valueOf(MapUtils.getStr(leadUser, "totalPrincipal"));
            if ("0".equals(MapUtils.getStr(leadUser, "interestType"))) {
                monthIncome += interest * totalPrincipal / 100;
            } else if ("1".equals(MapUtils.getStr(leadUser, "interestType"))) {
                Date startTime = parse(MapUtils.getStr(leadUser, "startTime"));
                Date endTime = parse(MapUtils.getStr(leadUser, "endTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                monthIncome += range * interest * totalPrincipal / 100;
            }
        }
        return monthIncome;
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
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }
    private String relatveDay(Date date,int offset){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + offset);
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }

    private String relatveDay(String relatveDay, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parse(relatveDay));
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + offset);
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }

    public List<Map> getPeriodById(Map param) {
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
                map.put("income",income);
            }else if("1".equals(MapUtils.getStr(map,"interestType"))){
                Date startTime = parse(MapUtils.getStr(map, "numStartTime"));
                Date endTime = parse(MapUtils.getStr(map, "numEndTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                double income=totalPrincipal*interest*range/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",income);
            }else if("2".equals(MapUtils.getStr(map,"interestType"))){
                int cycle=MapUtils.getInt(map,"cycle");
                double income=totalPrincipal*interest*7/100;
                income+=totalPrincipal/cycle;
                map.put("income",income);
            }
        });
        return periods;
    }

    public Integer updatePayNo(Map map) {
        Map period=new Dql().selectFirst("findLeadById").params(map.get("id")).execute();
        map.put("interestType",MapUtils.getStr(period,"interestType"));
        return new Dql().update("updatePayNum").params(map).execute();
    }

    public List<Map> periodListInit(EqlPage page) {
        List<Map> leadList = new Dql().select("periodList").limit(page).execute();
        leadList.stream().forEach(map->{
            double totalPrincipal=(double)map.get("totalPrincipal");
            double interest= (double)map.get("interest");
            String isLastNum =MapUtils.getStr(map,"isLastNum");
            if("0".equals(MapUtils.getStr(map,"interestType"))){
                double income=totalPrincipal*interest/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",income);
            }else if("1".equals(MapUtils.getStr(map,"interestType"))){
                Date startTime = parse(MapUtils.getStr(map, "numStartTime"));
                Date endTime = parse(MapUtils.getStr(map, "numEndTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                double income=totalPrincipal*interest*range/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",income);
            }else if("2".equals(MapUtils.getStr(map,"interestType"))){
                int cycle=MapUtils.getInt(map,"cycle");
                double income=totalPrincipal*interest*7/100;
                income+=totalPrincipal/cycle;
                map.put("income",income);
            }
        });
        return leadList;
    }

    public int getOverDueNum() {
        List<Map> overDueleadList = new Dql().select("periodList").params(MapUtils.of("overDue", "1")).execute();
        return overDueleadList.size();
    }

    public List<Map> getOverDue(Map param, EqlPage page) {
        List<Map> leadList = new Dql().select("periodList").params(param).limit(page).execute();
        leadList.stream().forEach(map->{
            double totalPrincipal=(double)map.get("totalPrincipal");
            double interest= (double)map.get("interest");
            String isLastNum =MapUtils.getStr(map,"isLastNum");
            if("0".equals(MapUtils.getStr(map,"interestType"))){
                double income=totalPrincipal*interest/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",income);
            }else if("1".equals(MapUtils.getStr(map,"interestType"))){
                Date startTime = parse(MapUtils.getStr(map, "numStartTime"));
                Date endTime = parse(MapUtils.getStr(map, "numEndTime"));
                Long time = endTime.getTime() - startTime.getTime();
                int range = (int) (time / 24 / 3600 / 1000);
                double income=totalPrincipal*interest*range/100;
                if("1".equals(isLastNum)){
                    income+=totalPrincipal;
                }
                map.put("income",income);
            }else if("2".equals(MapUtils.getStr(map,"interestType"))){
                int cycle=MapUtils.getInt(map,"cycle");
                double income=totalPrincipal*interest*7/100;
                income+=totalPrincipal/cycle;
                map.put("income",income);
            }
        });
        return leadList;
    }

    public int getToTimeNum() {
        List<Map> toTimeleadList = new Dql().select("periodList").params(MapUtils.of("toTime", "1")).execute();
        return toTimeleadList.size();
    }
}
