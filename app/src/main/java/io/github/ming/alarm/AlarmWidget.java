package io.github.ming.alarm;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.*;

public class AlarmWidget extends AppWidgetProvider {
    public static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,AlarmWidget.class));}
    public static boolean hasWidget(Context c){return ids(c).length>0;}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { update(c); }
    @Override public void onDeleted(Context c,int[] ids){Store.prefs(c).edit().remove("widgetPinPending").apply();}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if("io.github.ming.alarm.WIDGET_PINNED".equals(i.getAction())){Store.prefs(c).edit().remove("widgetPinPending").apply();update(c);}}
    public static void update(Context c) {
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        int[] ids=ids(c);Arrays.sort(ids);
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
            v.setImageViewResource(R.id.widget_character,R.drawable.character_icon);
            Intent data=new Intent(c,WidgetListService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
            data.setData(android.net.Uri.parse("qingling://widget/"+id));
            v.setRemoteAdapter(R.id.widget_list,data);v.setEmptyView(R.id.widget_list,R.id.widget_empty);
            PendingIntent template=PendingIntent.getActivity(c,2000+id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            v.setPendingIntentTemplate(R.id.widget_list,template);
            if(ids.length>1 && id!=ids[0]){
                v.setTextViewText(R.id.widget_time,"已添加闹钟表");
                v.setTextViewText(R.id.widget_detail,"请保留第一个组件并移除此旧副本");
                v.setViewVisibility(R.id.widget_list,android.view.View.GONE);
            }
            m.updateAppWidget(id,v);
            m.notifyAppWidgetViewDataChanged(id,R.id.widget_list);
        }
    }
}
