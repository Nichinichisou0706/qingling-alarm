package io.github.ming.alarm;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.io.File;
import java.util.*;

public final class SettingsPanel {
    private static void open(Activity a,Intent i){
        try{a.startActivity(i);}catch(Exception e){Toast.makeText(a,"系统未提供此入口，请在应用管理 → 青铃手动设置",Toast.LENGTH_LONG).show();}
    }
    private static void row(Activity a,LinearLayout page,String title,boolean granted,String detail,Runnable action){
        LinearLayout card=Ui.card(a,page,android.graphics.Color.WHITE);
        card.addView(Ui.text(a,(granted?"✓ ":"● 缺少 / 待确认：")+title,17,granted?Ui.GREEN:Ui.RED));
        card.addView(Ui.text(a,detail,13,granted?Ui.MUTED:Ui.RED));
        card.addView(Ui.button(a,granted?"查看设置":"去开启",false,action));
    }
    // Alarm reliability is the core function; the user explicitly chooses the system exemption dialog.
    @android.annotation.SuppressLint("BatteryLife")
    public static void populate(MainActivity a,LinearLayout page){
        page.addView(Ui.text(a,"青 铃   /   系统设置",13,Ui.GREEN));
        page.addView(Ui.title(a,"让每一次唤醒\n都被认真守护。",27));
        page.addView(Ui.text(a,"红色表示缺少权限或尚未确认。返回此页后自动重新检查。",14,Ui.RED));
        Uri pkg=Uri.parse("package:"+a.getPackageName());
        row(a,page,"精确闹钟",Scheduler.permitted(a),"用于准时唤醒设备。",()->{if(Build.VERSION.SDK_INT>=31)open(a,new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,pkg));});
        NotificationManager nm=a.getSystemService(NotificationManager.class);
        NotificationChannel channel=nm.getNotificationChannel(AlarmService.CHANNEL);
        boolean notifications=nm.areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
        row(a,page,"闹铃与锁屏通知",notifications,"允许通知，并保持“正在响铃”通知类别开启。",()->open(a,new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName())));
        boolean fullscreen=Build.VERSION.SDK_INT<34||nm.canUseFullScreenIntent();
        row(a,page,"全屏提醒",fullscreen,"允许锁屏时显示停止与稍后提醒按钮。",()->{if(Build.VERSION.SDK_INT>=34)open(a,new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,pkg));});
        boolean battery=a.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(a.getPackageName());
        row(a,page,"电池优化白名单",battery,"减少待机时的后台限制；HyperOS 应用省电策略还需选择“无限制”。",()->open(a,new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,pkg)));
        boolean restricted=Build.VERSION.SDK_INT>=28&&a.getSystemService(ActivityManager.class).isBackgroundRestricted();
        row(a,page,"系统后台运行",!restricted,restricted?"系统当前限制青铃后台运行。":"Android 未报告后台运行受限。",()->open(a,new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,pkg)));
        manual(a,page,"autoStart","HyperOS 后台自启动","在自启动管理中允许青铃。",new Intent().setComponent(new ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity")));
        manual(a,page,"hyperBattery","HyperOS 无限省电 / 锁定任务","应用省电策略设为“无限制”，并在最近任务中锁定青铃。",new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,pkg));
        manual(a,page,"hyperPopup","HyperOS 锁屏与后台弹窗","在其他权限中允许锁屏显示、后台弹出界面（如果有这些选项）。",new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,pkg));
        LinearLayout audio=Ui.card(a,page,Ui.PALE);audio.addView(Ui.title(a,"耳机与声音检查",18));
        audio.addView(Ui.text(a,AudioOutput.available(a)+"\n"+Store.prefs(a).getString("audioRoute","尚未试听"),13,Ui.GREEN));
        audio.addView(Ui.text(a,"蓝牙应开启“媒体音频”，不能只连接通话音频。青铃优先选择已连接的蓝牙／有线媒体耳机，不使用蓝牙通话通道。请同时检查 HyperOS 音量面板中的单独应用音量。",13,Ui.MUTED));
        page.addView(Ui.text(a,"系统可回收应用进程，无法保证永久保活。闹钟由系统精确调度并在重启后恢复；强行停止、关机或厂商限制仍会影响提醒。编辑草稿自动保存在本机，去系统设置或网易云后可恢复。",13,Ui.MUTED));
        page.addView(Ui.button(a,"清理未使用音乐缓存",false,()->{
            Set<String> used=new HashSet<>();for(Alarm alarm:Store.all(a))used.addAll(alarm.tracks);
            try{used.addAll(Alarm.from(new org.json.JSONObject(Store.prefs(a).getString("draft","{}"))).tracks);}catch(Exception ignored){}
            long bytes=0;File[] files=Store.music(a).listFiles();if(files!=null)for(File f:files)if(!used.contains(f.getName())&&!f.getName().endsWith(".part")){long n=f.length();if(f.delete())bytes+=n;}
            Toast.makeText(a,"已清理 "+bytes/1024/1024+" MB",Toast.LENGTH_LONG).show();
        }));
    }
    private static void manual(MainActivity a,LinearLayout page,String key,String title,String instructions,Intent intent){
        boolean checked=Store.prefs(a).getBoolean(key,false);
        row(a,page,title,checked,instructions+"\n"+(checked?"已由你手动确认；系统不提供自动读取接口。":"系统不提供自动读取接口，请打开设置检查。"),()->open(a,intent));
        CheckBox confirm=new CheckBox(a);confirm.setText(a.getString(R.string.manual_permission,title));confirm.setTextColor(checked?Ui.GREEN:Ui.RED);confirm.setChecked(checked);page.addView(confirm);
        confirm.setOnCheckedChangeListener((button,on)->{Store.prefs(a).edit().putBoolean(key,on).apply();a.showSettings();});
    }
}
