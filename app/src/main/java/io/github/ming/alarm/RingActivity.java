package io.github.ming.alarm;
import android.app.Activity;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class RingActivity extends Activity {
    private int id;
    private final Handler stateHandler=new Handler(Looper.getMainLooper());
    private final Runnable stateCheck=new Runnable(){public void run(){
        int active=AlarmService.activeId;
        if(active==Integer.MIN_VALUE){finish();return;}
        if(active!=id){getIntent().putExtra("id",active);show();}
        stateHandler.postDelayed(this,500);
    }};
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        show();
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);show();}
    @Override protected void onResume(){super.onResume();stateHandler.post(stateCheck);}
    @Override protected void onPause(){stateHandler.removeCallbacks(stateCheck);super.onPause();}
    private void show(){
        id=getIntent().getIntExtra("id",-1);Alarm alarm=Store.get(this,id);
        LinearLayout page=Ui.page(this);page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(Ui.text(this,"Q I N G L I N G   /   MORNING CALL",12,Ui.GREEN));
        page.addView(new MascotView(this),new LinearLayout.LayoutParams(Ui.dp(this,210),Ui.dp(this,220)));
        page.addView(Ui.title(this,new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date()),64));
        page.addView(Ui.title(this,id<0?"试听喜欢的旋律":alarm==null?"该醒来啦":alarm.label,24));
        page.addView(Ui.text(this,"慢慢睁开眼，今天也值得期待。",14,Ui.MUTED));
        page.addView(Ui.button(this,"我醒了 · 停止",true,()->{AlarmService.dismiss(this,id,false);finish();}));
        if(id>0)page.addView(Ui.button(this,"再休息 5 分钟",false,()->{AlarmService.dismiss(this,id,true);finish();}));
        page.addView(Ui.text(this,"跟随媒体输出 · 10 分钟后自动停止",12,Ui.MUTED));
    }
    @Override protected void onDestroy(){stateHandler.removeCallbacksAndMessages(null);super.onDestroy();}
}
