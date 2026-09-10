package io.github.ming.alarm;
import android.content.*;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        String action=intent.getAction(); int id=intent.getIntExtra("id",0);
        if("STOP".equals(action) || "SNOOZE".equals(action)) {
            AlarmService.dismiss(c,id,"SNOOZE".equals(action)); return;
        }
        Alarm a=Scheduler.consume(c,id,intent.getLongExtra("when",0),"SNOOZE_FIRE".equals(action));
        if(a==null) return;
        try { c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",a.id)); }
        catch(RuntimeException e) { Store.status(c,"系统阻止了闹铃启动，请检查自启动与省电设置："+e.getClass().getSimpleName()); }
    }
}
