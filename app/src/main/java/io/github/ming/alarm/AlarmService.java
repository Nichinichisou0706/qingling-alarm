package io.github.ming.alarm;

import android.app.*;
import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.net.Uri;
import android.os.*;
import java.io.File;

public class AlarmService extends Service {
    public static final String CHANNEL="ringing-v1", CLOSED="io.github.ming.alarm.CLOSED";
    public static volatile int activeId=Integer.MIN_VALUE;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wake;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private Alarm alarm;
    private int index, failures;
    private boolean mutedForDisconnect, registered;
    private final BroadcastReceiver noisy=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {
            mutedForDisconnect=true; releasePlayer(); vibrate();
            Store.status(c,"耳机断开：已暂停音乐并改为振动，避免突然外放");
            getSystemService(NotificationManager.class).notify(1,notification("耳机断开 · 已改为振动"));
        }
    };
    public static void dismiss(Context c,int id,boolean snooze) {
        if(activeId!=id) return;
        if(snooze && id>0) {
            Alarm a=Store.get(c,id);
            if(a!=null && Scheduler.permitted(c)) Scheduler.snooze(c,a);
            else Store.status(c,"无法稍后提醒：请开启精确闹钟权限");
        }
        c.stopService(new Intent(c,AlarmService.class));
    }
    @Override public void onCreate() {
        super.onCreate(); audio=getSystemService(AudioManager.class);
        vibrator=getSystemService(Vibrator.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"正在响铃",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("闹钟全屏提醒；铃声通过媒体通道单独播放");
        channel.setSound(null,null); channel.enableVibration(false); channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        IntentFilter filter=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(noisy,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(noisy,filter);
        registered=true;
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent==null) {stopSelf();return START_NOT_STICKY;}
        int id=intent.getIntExtra("id",-1);
        // A preview must never replace a real ringing alarm.
        if(id<0 && activeId>0) return START_NOT_STICKY;
        cleanupPlayback();
        alarm=id<0 ? Alarm.from(parse(intent.getStringExtra("preview"))) : Store.get(this,id);
        if(alarm==null) { stopSelf(); return START_NOT_STICKY; }
        activeId=id; index=0; failures=0; mutedForDisconnect=false;
        startForeground(1,notification(id<0?"试听 · 30 秒后结束":alarm.label));
        wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"qingling:ring");
        wake.acquire(11*60_000L);
        if(alarm.mode!=0) vibrate();
        if(alarm.mode!=1) {
            AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{
                    if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if(player!=null && player.isPlaying()) player.pause(); vibrate();
                    } else if(change==AudioManager.AUDIOFOCUS_GAIN && player!=null && !mutedForDisconnect) {
                        try { player.start(); if(alarm.mode==0) vibrator.cancel(); } catch(IllegalStateException ignored) { fallback(); }
                    }
                }).build();
            int result=audio.requestAudioFocus(focus);
            if(result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED) playNext();
            else { vibrate(); Store.status(this,"音乐焦点被通话或其他应用占用，本次改为振动"); }
        }
        if(id>0) Store.status(this,"最近响铃："+alarm.time()+" · "+alarm.label);
        handler.postDelayed(()->{Store.status(this,id<0?"试听已结束":"闹铃已在 10 分钟后自动停止");stopSelf();},id<0?30_000:600_000);
        return START_NOT_STICKY;
    }
    private org.json.JSONObject parse(String s) {
        try{return new org.json.JSONObject(s==null?"{}":s);} catch(Exception e){return new org.json.JSONObject();}
    }
    private Notification notification(String text) {
        Intent open=new Intent(this,RingActivity.class).putExtra("id",activeId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent screen=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("青铃 · "+text).setContentText("媒体音量 · 跟随当前音频输出")
            .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true).setContentIntent(screen).setFullScreenIntent(screen,true)
            .addAction(new Notification.Action.Builder(null,"停止",action("STOP")).build());
        if(activeId>0) b.addAction(new Notification.Action.Builder(null,"稍后 5 分钟",action("SNOOZE")).build());
        return b.build();
    }
    private PendingIntent action(String action) {
        return PendingIntent.getBroadcast(this,0,new Intent(this,AlarmReceiver.class).setAction(action).putExtra("id",activeId),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void vibrate() {
        if(vibrator!=null && vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,500,250,500,1200},0));
    }
    private void playNext() {
        if(mutedForDisconnect) return;
        if(alarm.tracks.isEmpty() || failures>=alarm.tracks.size()) { fallback(); return; }
        String path=alarm.tracks.get(index++ % alarm.tracks.size());
        prepare(Uri.fromFile(new File(Store.music(this),path)),false);
    }
    private void fallback() {
        if(mutedForDisconnect) return;
        if(!alarm.tracks.isEmpty()) Store.status(this,"导入音乐无法读取，已使用内置晨间微光");
        prepare(null,true);
    }
    private void prepare(Uri uri,boolean builtIn) {
        releasePlayer();
        try {
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            if(builtIn) {
                try(AssetFileDescriptor fd=getResources().openRawResourceFd(R.raw.morning)) {player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
            } else player.setDataSource(this,uri);
            player.setLooping(builtIn);
            player.setVolume(alarm.volume/100f,alarm.volume/100f);
            player.setOnPreparedListener(mp->{
                if(mp!=player || mutedForDisconnect) return;
                mp.start();
                if(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0) {
                    vibrate(); Store.status(this,"媒体音量为 0：本次同时振动，请调高媒体音量");
                }
            });
            player.setOnCompletionListener(mp->{ failures=0; playNext(); });
            player.setOnErrorListener((mp,what,extra)->{ handler.post(()->failed(builtIn)); return true; });
            player.prepareAsync();
        } catch(Exception e) { failed(builtIn); }
    }
    private void failed(boolean builtIn) {
        releasePlayer();
        if(builtIn) {vibrate(); Store.status(this,"音频播放失败，本次改为振动");}
        else {failures++;playNext();}
    }
    private void releasePlayer() { if(player!=null) {player.release();player=null;} }
    private void cleanupPlayback() {
        handler.removeCallbacksAndMessages(null); releasePlayer();
        if(vibrator!=null) vibrator.cancel();
        if(focus!=null) {audio.abandonAudioFocusRequest(focus);focus=null;}
        if(wake!=null && wake.isHeld()) wake.release(); wake=null;
    }
    @Override public void onDestroy() {
        cleanupPlayback(); if(registered) unregisterReceiver(noisy);
        activeId=Integer.MIN_VALUE; sendBroadcast(new Intent(CLOSED).setPackage(getPackageName()));
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
