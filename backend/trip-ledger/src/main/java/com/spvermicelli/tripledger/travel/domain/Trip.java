package com.spvermicelli.tripledger.travel.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;

/** Trip aggregate. Version protects the entire schedule from lost updates. */
public record Trip(Long id, long version, String name, String destination, LocalDate startDate,
    LocalDate endDate, int people, long budgetCent, String pace, String style, String stay,
    Long bookId, boolean archived, List<Activity> activities) {
    public record Activity(String id, int day, String title, String time, int duration, String note,
        String color, boolean locked, boolean done, String poiId, Double longitude, Double latitude, String coordinateSystem) {
        public Activity(String id,int day,String title,String time,int duration,String note,String color,boolean locked,boolean done,String poiId,Double longitude,Double latitude){this(id,day,title,time,duration,note,color,locked,done,poiId,longitude,latitude,null);}
    }
    public Trip {
        activities = activities == null ? List.of() : List.copyOf(activities);
    }
    public int days() { return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1; }
    public Trip identified(Long id, long version) {
        return new Trip(id,version,name,destination,startDate,endDate,people,budgetCent,pace,style,stay,bookId,archived,activities);
    }
    public Trip planned(List<Activity> items, int people, long budget, String pace, String style, String stay) {
        return new Trip(id,version,name,destination,startDate,endDate,people,budget,pace,style,stay,bookId,archived,items);
    }
    public void validate() {
        require(name!=null&&!name.isBlank()&&name.length()<=50,"名称不能为空且不超过50字");
        require(destination!=null&&!destination.isBlank()&&destination.length()<=50,"请填写目的地");
        require(startDate!=null&&endDate!=null&&days()>=1&&days()<=30,"行程须为1至30天");
        require(people>=1&&people<=30&&budgetCent>=0&&budgetCent<=100_000_000L,"人数或预算超出范围");
        require(Set.of("relaxed","balanced","packed").contains(pace==null?"":pace),"节奏无效");
        require(Set.of("classic","culture","nature").contains(style==null?"":style),"旅行偏好无效");
        require(stay!=null&&stay.length()<=50,"住宿偏好无效");
        require(activities.size()<=180,"行程卡最多180张");
        Set<String> ids=new HashSet<>();
        for(Activity a:activities) {
            require(a.id()!=null&&a.id().length()<=64&&ids.add(a.id()),"行程卡ID重复或无效");
            require(a.day()>=0&&a.day()<days(),"行程卡超出日期范围，请先移走对应安排");
            require(a.title()!=null&&!a.title().isBlank()&&a.title().length()<=100,"地点名称无效");
            require(a.note()==null||a.note().length()<=500,"备注过长");
            require(a.duration()>=15&&a.duration()<=600,"停留须为15至600分钟");
            require(Set.of("blue","rose","sage").contains(a.color()==null?"":a.color()),"卡片颜色无效");
            int minute=time(a.time()); require(minute+a.duration()<=1440,"活动不能跨越当天24点");
            require((a.longitude()==null)==(a.latitude()==null),"经纬度必须同时填写");
            require(a.coordinateSystem()==null||Set.of("WGS84","GCJ-02").contains(a.coordinateSystem()),"坐标类型无效");
            if(a.longitude()!=null)require(Double.isFinite(a.longitude())&&Double.isFinite(a.latitude())&&Math.abs(a.longitude())<=180&&Math.abs(a.latitude())<=90,"坐标无效");
        }
        for(int d=0;d<days();d++) {
            final int index=d;
            List<Activity> items=activities.stream().filter(a->a.day()==index).sorted(Comparator.comparingInt(a->time(a.time()))).toList();
            require(items.size()<=12,"每天最多12个地点");
            for(int i=1;i<items.size();i++)require(time(items.get(i-1).time())+items.get(i-1).duration()<=time(items.get(i).time()),"行程时间重叠");
        }
    }
    public void validateChange(Trip before) {
        validate();
        if(before.archived()&&archived&&!equals(before))throw new BusinessException(ErrorCode.INVALID_STATUS,"请先恢复归档行程");
        for(Activity old:before.activities()) if(old.locked()||old.done()) {
            Activity next=activities.stream().filter(a->a.id().equals(old.id())).findFirst().orElse(null);
            require(next!=null,"请先解锁或取消打卡再删除安排");
            if(old.locked())require(old.day()==next.day()&&Objects.equals(old.title(),next.title())&&Objects.equals(old.time(),next.time())&&old.duration()==next.duration()&&Objects.equals(old.longitude(),next.longitude())&&Objects.equals(old.latitude(),next.latitude()),"请先解锁再修改安排");
        }
    }
    public static int time(String value) {
        try {LocalTime t=LocalTime.parse(value);return t.getHour()*60+t.getMinute();}
        catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_PARAM,"时间格式无效");}
    }
    public static void require(boolean ok,String message){if(!ok)throw new BusinessException(ErrorCode.INVALID_PARAM,message);}
}
