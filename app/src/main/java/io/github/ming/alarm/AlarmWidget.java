package io.github.ming.alarm;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.*;

public class AlarmWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { update(c); }
    public static void update(Context c) {
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        int[] ids=m.getAppWidgetIds(new ComponentName(c,AlarmWidget.class));
        List<Alarm> alarms=Store.all(c); long next=Long.MAX_VALUE; Alarm soon=null; int count=0;
        for(Alarm a:alarms) {
            if(a.enabled) count++;
            long at=a.snoozeAt>0 ? a.snoozeAt : a.enabled ? a.nextAt : 0;
            if(a.enabled && a.nextAt>0 && at>a.nextAt) at=a.nextAt;
            if(at>0 && at<next) {next=at;soon=a;}
        }
        String detail;
        if(soon==null) detail="暂无待响闹钟 · 点击设置";
        else {
            detail=new SimpleDateFormat("M月d日 E",Locale.CHINA).format(new Date(next))+" · "+soon.label+"\n"+soon.modeName()+" · 已开启 "+count+" 个";
            StringBuilder rest=new StringBuilder();
            for(Alarm a:alarms) if(a.enabled && a.id!=soon.id && rest.length()<40) rest.append("  ").append(a.time());
            if(rest.length()>0) detail+="\n还有"+rest;
        }
        for(int id:ids) {
            RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
            v.setTextViewText(R.id.widget_time,soon==null?"— — : — —":new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(next)));
            v.setTextViewText(R.id.widget_detail,detail);
            v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id,v);
        }
    }
}
