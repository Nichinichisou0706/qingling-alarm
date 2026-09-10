package io.github.ming.alarm;

import java.time.LocalTime;

/** Always follows the device's current local time and timezone. */
public enum DayPeriod {
    MORNING("早安，\n今天也陪着你。", "让喜欢的旋律开启新一天", "挥手问早", "新的一天，按自己的节奏出发。"),
    NOON("午安，\n歇一会儿吧。", "留一点时间给午休和好心情", "午间小憩", "午间约定到了，慢慢舒展一下。"),
    AFTERNOON("下午好，\n一起慢慢来。", "忙碌之余，也别忘了照顾自己", "专注陪伴", "到了约定的时间，给自己一点动力。"),
    EVENING("晚上好，\n今天辛苦了。", "把接下来的时间留给喜欢的事", "迎接晚归", "晚间约定到了，放松一下吧。"),
    NIGHT("夜深了，\n愿你好梦。", "设好提醒，让心事轻轻放下", "安静休息", "夜深了，轻轻提醒你：约定的时间到了。 ");

    public final String greeting, subtitle, state, ringMessage;
    DayPeriod(String greeting,String subtitle,String state,String ringMessage){
        this.greeting=greeting;this.subtitle=subtitle;this.state=state;this.ringMessage=ringMessage;
    }
    public static DayPeriod now(){return atHour(LocalTime.now().getHour());}
    public static DayPeriod atHour(int hour){
        if(hour<0||hour>23)throw new IllegalArgumentException("hour");
        if(hour<5||hour>=22)return NIGHT;
        if(hour<11)return MORNING;
        if(hour<14)return NOON;
        if(hour<18)return AFTERNOON;
        return EVENING;
    }
}
