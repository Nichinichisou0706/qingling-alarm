package io.github.ming.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.File;
import java.util.*;

public final class Store {
    public static Context device(Context c) { return c.createDeviceProtectedStorageContext(); }
    static SharedPreferences prefs(Context c) { return device(c).getSharedPreferences("alarms", Context.MODE_PRIVATE); }
    public static synchronized List<Alarm> all(Context c) {
        List<Alarm> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("items","[]"));
            for(int i=0;i<a.length();i++) list.add(Alarm.from(a.getJSONObject(i)));
        } catch(JSONException e) { status(c,"闹钟数据读取失败，请重新检查设置"); }
        list.sort(Comparator.comparingInt(a -> a.hour*60+a.minute));
        return list;
    }
    public static synchronized Alarm get(Context c,int id) {
        for(Alarm a:all(c)) if(a.id==id) return a;
        return null;
    }
    public static synchronized int newId(Context c) {
        int id = prefs(c).getInt("counter",0)+1;
        if(!prefs(c).edit().putInt("counter",id).commit()) throw new IllegalStateException("无法保存闹钟编号，请检查存储空间");
        return id;
    }
    public static synchronized void save(Context c, Alarm alarm) {
        List<Alarm> list=all(c); list.removeIf(a->a.id==alarm.id); list.add(alarm); write(c,list);
    }
    public static synchronized void delete(Context c,int id) {
        List<Alarm> list=all(c); list.removeIf(a->a.id==id); write(c,list);
    }
    private static void write(Context c,List<Alarm> list) {
        JSONArray a=new JSONArray(); for(Alarm alarm:list) a.put(alarm.json());
        if (!prefs(c).edit().putString("items",a.toString()).commit()) throw new IllegalStateException("保存失败：存储空间不足");
    }
    public static File music(Context c) {
        File f=new File(device(c).getFilesDir(),"music");
        if (!f.exists() && !f.mkdirs()) throw new IllegalStateException("无法创建音乐目录");
        return f;
    }
    public static void status(Context c,String value) { prefs(c).edit().putString("status",value).apply(); }
}
