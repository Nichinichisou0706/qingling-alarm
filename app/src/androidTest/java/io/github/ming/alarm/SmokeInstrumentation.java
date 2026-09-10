package io.github.ming.alarm;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicReference;

/** Run only on a disposable test device. Exercises real AlarmManager -> receiver -> service. */
public class SmokeInstrumentation extends Instrumentation {
    private int checks;
    private boolean network;
    private boolean demo;
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private void main(Runnable r) throws Throwable {
        AtomicReference<Throwable> error=new AtomicReference<>();
        runOnMainSync(()->{try{r.run();}catch(Throwable t){error.set(t);}});
        if(error.get()!=null)throw error.get();
    }
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);network=arguments!=null&&"true".equals(arguments.getString("network"));demo=arguments!=null&&"true".equals(arguments.getString("demo"));start();}
    @Override public void onStart(){
        Context c=getTargetContext();Bundle result=new Bundle();
        if(demo){
            String[] names={"晨间唤醒","午休时间","散步和音乐","阅读片刻","运动时间","喝杯水吧","日语练习","晚安计划"};
            for(int i=0;i<names.length;i++){Alarm a=new Alarm();a.id=910000+i;a.hour=7+i*2;a.minute=30;a.days=i==0?31:127;a.mode=i%3;a.enabled=i%3!=1;a.label=names[i];Scheduler.save(c,a);}
            finish(Activity.RESULT_OK,new Bundle());return;
        }
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
            check(Store.prefs(c).getString("audioDecode","").startsWith("PCM"),"Imported WAV uses decoded PCM path rather than fallback");
            main(()->AlarmService.dismiss(c,-1,false));Thread.sleep(500);
            new java.io.File(Store.music(c),imported).delete();
            if(network){
                MusicImport.Playlist list=MusicImport.load("3778678");
                check(!list.songs.isEmpty(),"NetEase public playlist parsed on Android");
                MusicImport.Playlist shared=MusicImport.shared("https://music.163.com/song?id=1303464858");
                check(shared.songs.size()==1&&shared.songs.get(0).id==1303464858L,"Song shared from NetEase resolves to one selected track");
                MusicImport.Song available=new MusicImport.Song(1303464858L,"于是");MusicImport.probe(available);
                check(available.availability==2,"Availability checked before selection");
                String cached=MusicImport.cache(c,new MusicImport.Song(1303464858L,"于是"));
                check(new java.io.File(Store.music(c),cached).length()>1000,"NetEase audio cached and decoded on Android");
                main(()->{Alarm p=new Alarm();p.mode=0;p.tracks.add(cached);c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",-1).putExtra("preview",p.json().toString()));});
                Thread.sleep(1500);
                check(c.getSystemService(android.media.AudioManager.class).isMusicActive(),"Cached NetEase audio plays on media stream");
                check(Store.prefs(c).getString("audioDecode","").startsWith("PCM"),"Compressed NetEase audio decoded to PCM without offload");
                main(()->AlarmService.dismiss(c,-1,false));Thread.sleep(500);
                new java.io.File(Store.music(c),cached).delete();
            }
            main(()->{
                for(int i=0;i<8;i++){Alarm row=new Alarm();row.id=900100+i;row.hour=i;row.enabled=i%2==0;Store.save(c,row);}
                WidgetListService.Rows rows=new WidgetListService.Rows(c);rows.onCreate();
                check(rows.getCount()>=8,"Widget contains full alarm table including disabled rows");
                check(rows.hasStableIds()&&rows.getViewAt(0)!=null,"Widget scroll rows have stable IDs and RemoteViews");
                for(int i=0;i<8;i++)Store.delete(c,900100+i);
            });
            MainActivity activity=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(()->{activity.selectTab(2);check(activity.currentTab()==2,"System permissions are a separate module");activity.selectTab(1);check(activity.currentTab()==1,"Alarm list is a separate module");activity.selectTab(0);check(activity.currentTab()==0,"Alarm editor is a separate module");activity.onBackPressed();});
            android.view.accessibility.AccessibilityNodeInfo root=null;
            long dialogDeadline=SystemClock.elapsedRealtime()+6000;
            do{
                Thread.sleep(250);root=getUiAutomation().getRootInActiveWindow();
                if(root!=null&&!root.findAccessibilityNodeInfosByText("保存并返回").isEmpty())break;
            }while(SystemClock.elapsedRealtime()<dialogDeadline);
            check(root!=null&&!root.findAccessibilityNodeInfosByText("保存并返回").isEmpty(),"Editor back offers save before leaving");
            if(root!=null){java.util.List<android.view.accessibility.AccessibilityNodeInfo> cancel=root.findAccessibilityNodeInfosByText("不保存");if(!cancel.isEmpty())cancel.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);}
            main(activity::finish);
            result.putString("stream","PASS: "+checks+" device checks\n");finish(Activity.RESULT_OK,result);
        } catch(Throwable t) {
            result.putString("stream","FAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
