package io.github.ming.alarm;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.util.concurrent.atomic.AtomicReference;

/** Run only on a disposable test device. Exercises real AlarmManager -> receiver -> service. */
public class SmokeInstrumentation extends Instrumentation {
    private int checks;
    private boolean network;
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private void main(Runnable r) throws Throwable {
        AtomicReference<Throwable> error=new AtomicReference<>();
        runOnMainSync(()->{try{r.run();}catch(Throwable t){error.set(t);}});
        if(error.get()!=null)throw error.get();
    }
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);network=arguments!=null&&"true".equals(arguments.getString("network"));start();}
    @Override public void onStart(){
        Context c=getTargetContext();Bundle result=new Bundle();
        try {
            check(Scheduler.permitted(c),"Exact alarm permission granted");
            main(()->{
                Alarm a=new Alarm();a.id=900001;a.label="自动化测试";a.days=31;a.mode=1;
                Scheduler.save(c,a);
                Alarm stored=Store.get(c,a.id);
                check(stored!=null&&stored.days==31&&stored.mode==1,"Persistent alarm round trip");
                check(stored.nextAt>System.currentTimeMillis(),"Future occurrence scheduled");
                check(Scheduler.consume(c,a.id,stored.nextAt+1,false)==null,"Reject stale trigger");
                Scheduler.cancel(c,a);Store.delete(c,a.id);
                for(int mode=0;mode<3;mode++){
                    Alarm b=new Alarm();b.id=900010+mode;b.mode=mode;Store.save(c,b);
                    check(Store.get(c,b.id).mode==mode,"Mode preserved: "+mode);Store.delete(c,b.id);
                }
                Alarm timed=new Alarm();timed.id=900002;timed.mode=1;timed.nextAt=System.currentTimeMillis()+3000;
                Store.save(c,timed);Scheduler.restore(c,false);
                check(c.getSystemService(AlarmManager.class).getNextAlarmClock()!=null,"System alarm clock registered");
            });
            long until=System.currentTimeMillis()+15_000;
            while(AlarmService.activeId!=900002&&System.currentTimeMillis()<until)Thread.sleep(200);
            check(AlarmService.activeId==900002,"Actual AlarmManager fired foreground vibration service");
            check(!Store.get(c,900002).enabled,"One-shot disabled after firing");
            main(()->AlarmService.dismiss(c,900002,true));Thread.sleep(500);
            check(Store.get(c,900002).snoozeAt>System.currentTimeMillis(),"Snooze scheduled");
            main(()->{Alarm a=Store.get(c,900002);Scheduler.cancel(c,a);Store.delete(c,a.id);AlarmWidget.update(c);});
            check(AlarmService.activeId==Integer.MIN_VALUE,"Dismiss releases active service");
            main(()->{
                Alarm p=new Alarm();p.mode=0;
                c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",-1).putExtra("preview",p.json().toString()));
            });Thread.sleep(1500);
            check(AlarmService.activeId==-1,"Music preview service started");
            check(c.getSystemService(android.media.AudioManager.class).isMusicActive(),"Built-in music uses media stream");
            main(()->AlarmService.dismiss(c,-1,false));
            Thread.sleep(500);
            String imported=MusicImport.local(c,android.net.Uri.parse("android.resource://"+c.getPackageName()+"/"+R.raw.morning));
            check(new java.io.File(Store.music(c),imported).length()>1000,"Local audio validated and copied");
            main(()->{Alarm p=new Alarm();p.mode=0;p.tracks.add(imported);c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",-1).putExtra("preview",p.json().toString()));});
            Thread.sleep(1500);
            check(c.getSystemService(android.media.AudioManager.class).isMusicActive(),"Imported local music plays");
            main(()->AlarmService.dismiss(c,-1,false));Thread.sleep(500);
            new java.io.File(Store.music(c),imported).delete();
            if(network){
                MusicImport.Playlist list=MusicImport.load("3778678");
                check(!list.songs.isEmpty(),"NetEase public playlist parsed on Android");
                String cached=MusicImport.cache(c,new MusicImport.Song(1303464858L,"于是"));
                check(new java.io.File(Store.music(c),cached).length()>1000,"NetEase audio cached and decoded on Android");
                main(()->{Alarm p=new Alarm();p.mode=0;p.tracks.add(cached);c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",-1).putExtra("preview",p.json().toString()));});
                Thread.sleep(1500);
                check(c.getSystemService(android.media.AudioManager.class).isMusicActive(),"Cached NetEase audio plays on media stream");
                main(()->AlarmService.dismiss(c,-1,false));Thread.sleep(500);
                new java.io.File(Store.music(c),cached).delete();
            }
            result.putString("stream","PASS: "+checks+" device checks\n");finish(Activity.RESULT_OK,result);
        } catch(Throwable t) {
            result.putString("stream","FAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
