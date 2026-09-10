package io.github.ming.alarm;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.ZoneId;

public final class Scheduler {
    public static boolean permitted(Context c) { return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    private static PendingIntent trigger(Context c,Alarm a,boolean snooze) {
        Intent i=new Intent(c,AlarmReceiver.class).setAction(snooze ? "SNOOZE_FIRE" : "FIRE")
            .putExtra("id",a.id).putExtra("when",snooze ? a.snoozeAt : a.nextAt);
        return PendingIntent.getBroadcast(c,a.id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void install(Context c,Alarm a,boolean snooze) {
        long at=snooze ? a.snoozeAt : a.nextAt;
        if(at<=0 || !permitted(c)) return;
        PendingIntent show=PendingIntent.getActivity(c,a.id,new Intent(c,MainActivity.class).putExtra("edit",a.id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),trigger(c,a,snooze));
    }
    public static void cancel(Context c,Alarm a) {
        AlarmManager m=c.getSystemService(AlarmManager.class); m.cancel(trigger(c,a,false)); m.cancel(trigger(c,a,true));
    }
    public static void save(Context c,Alarm a) {
        cancel(c,a); a.snoozeAt=0;
        a.nextAt=a.enabled ? AlarmTime.next(a.hour,a.minute,a.days,System.currentTimeMillis(),ZoneId.systemDefault()) : 0;
        Store.save(c,a); install(c,a,false); AlarmWidget.update(c);
    }
    public static void snooze(Context c,Alarm a) {
        a.snoozeAt=System.currentTimeMillis()+5*60_000L; Store.save(c,a); install(c,a,true); AlarmWidget.update(c);
    }
    public static synchronized Alarm consume(Context c,int id,long when,boolean snooze) {
        Alarm a=Store.get(c,id);
        if(a==null || when<=0 || (snooze ? a.snoozeAt!=when : !a.enabled || a.nextAt!=when)) return null;
        if(snooze) a.snoozeAt=0;
        else {
            if(a.days==0) a.enabled=false;
            a.nextAt=a.enabled ? AlarmTime.next(a.hour,a.minute,a.days,System.currentTimeMillis(),ZoneId.systemDefault()) : 0;
        }
        Store.save(c,a);
        if(!snooze) install(c,a,false);
        AlarmWidget.update(c); return a;
    }
    public static void restore(Context c,boolean clockChanged) {
        long now=System.currentTimeMillis();
        for(Alarm a:Store.all(c)) {
            if(a.enabled) {
                if(clockChanged || a.nextAt==0 || a.nextAt<now-600_000)
                    a.nextAt=AlarmTime.next(a.hour,a.minute,a.days,now,ZoneId.systemDefault());
                else if(a.nextAt<=now) a.nextAt=now+2000;
            } else a.nextAt=0;
            if(a.snoozeAt>0 && a.snoozeAt<=now) a.snoozeAt=a.snoozeAt>now-600_000 ? now+2000 : 0;
            Store.save(c,a); install(c,a,false); install(c,a,true);
        }
        AlarmWidget.update(c);
    }
}
