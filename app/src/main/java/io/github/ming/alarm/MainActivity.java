package io.github.ming.alarm;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private Alarm editing;
    private int tab=1;
    private TextView outputView;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView sourceView;
    private DayPeriod shownPeriod=DayPeriod.now();
    private TextView greetingView, subtitleView;
    private MascotView mascotView;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){
        if(outputView!=null)outputView.setText(getString(R.string.audio_diagnostic,AudioOutput.available(MainActivity.this),Store.prefs(MainActivity.this).getString("audioRoute","试听时显示实际输出"),Store.prefs(MainActivity.this).getString("audioDecode","")));
        DayPeriod period=DayPeriod.now();
        if(shownPeriod!=period){shownPeriod=period;if(greetingView!=null)greetingView.setText(period.greeting);if(subtitleView!=null)subtitleView.setText(period.subtitle);if(mascotView!=null)mascotView.setPeriod(period);}
        timer.postDelayed(this,1500);
    }};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);
        if(state!=null)tab=state.getInt("tab",1);
        if(state!=null && state.containsKey("editing")) {
            try {editing=Alarm.from(new org.json.JSONObject(state.getString("editing")));}catch(Exception ignored){}
        }
        Scheduler.restore(this,false);
        if(editing==null && Store.prefs(this).contains("draft"))try{editing=Alarm.from(new org.json.JSONObject(Store.prefs(this).getString("draft","{}")));}catch(Exception ignored){}
        render();handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private void handleIntent(Intent i){
        int id=i.getIntExtra("edit",0);Alarm a=Store.get(this,id);
        if(a!=null){editing=a;tab=0;editor();}i.removeExtra("edit");
        if(Intent.ACTION_SEND.equals(i.getAction())){
            String shared=i.getStringExtra(Intent.EXTRA_TEXT);i.setAction(Intent.ACTION_MAIN);i.removeExtra(Intent.EXTRA_TEXT);
            if(shared!=null){if(editing==null){editing=new Alarm();editing.id=Store.newId(this);}tab=0;editor();loadShared(shared);}
        }
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("tab",tab);if(editing!=null)out.putString("editing",editing.json().toString());}
    @Override protected void onResume(){super.onResume();if(tab==2)settings();else if(tab==1)home();timer.postDelayed(refresh,1500);}
    @Override protected void onPause(){timer.removeCallbacks(refresh);persistDraft();super.onPause();}
    public int currentTab(){return tab;}
    public void selectTab(int next){
        if(next==tab)return;
        tab=next;render();
    }
    private void render(){
        outputView=null;greetingView=null;subtitleView=null;mascotView=null;
        if(tab==0){if(editing==null){editing=new Alarm();editing.id=Store.newId(this);}editor();}
        else if(tab==2)settings();else home();
    }
    private void persistDraft(){
        if(editing!=null)Store.prefs(this).edit().putString("draft",editing.json().toString()).apply();
        else Store.prefs(this).edit().remove("draft").apply();
    }
    @Override protected void onDestroy(){worker.shutdownNow();timer.removeCallbacksAndMessages(null);super.onDestroy();}
    private void ui(Runnable r){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())r.run();});}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void message(String title,String text){new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("知道了",null).show();}
    private void home(){
        tab=1;outputView=null;LinearLayout page=Ui.page(this);
        page.addView(Ui.text(this,"青 铃   /   我的闹钟",13,Ui.GREEN));
        LinearLayout heading=Ui.row(this);page.addView(heading);
        LinearLayout words=Ui.column(this);heading.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        DayPeriod period=DayPeriod.now();shownPeriod=period;
        words.addView(greetingView=Ui.title(this,period.greeting,27));
        words.addView(subtitleView=Ui.text(this,period.subtitle,13,Ui.MUTED));
        heading.addView(mascotView=new MascotView(this),new LinearLayout.LayoutParams(Ui.dp(this,132),Ui.dp(this,205)));
        List<Alarm> alarms=Store.all(this);long next=Long.MAX_VALUE;Alarm soon=null;
        for(Alarm a:alarms){long at=a.enabled?a.nextAt:0;if(a.snoozeAt>0&&(at==0||a.snoozeAt<at))at=a.snoozeAt;if(at>0&&at<next){next=at;soon=a;}}
        LinearLayout hero=Ui.card(this,page,Ui.PALE);
        hero.addView(Ui.text(this,"下一次唤醒  /  UP NEXT",12,Ui.GREEN));
        if(soon==null){hero.addView(Ui.title(this,"等你定个好时间",27));hero.addView(Ui.text(this,"从一个属于自己的早晨开始。",14,Ui.MUTED));}
        else {
            hero.addView(Ui.title(this,new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(next)),48));
            long minutes=Math.max(1,(next-System.currentTimeMillis()+59999)/60000);
            hero.addView(Ui.text(this,new SimpleDateFormat("M月d日 E",Locale.CHINA).format(new Date(next))+" · "+soon.label,14,Ui.INK));
            hero.addView(Ui.text(this,"约 "+(minutes/60)+" 小时 "+(minutes%60)+" 分钟后 · "+soon.modeName(),13,Ui.MUTED));
        }
        page.addView(Ui.button(this,"＋  设定新闹钟",true,()->{editing=new Alarm();editing.id=Store.newId(this);tab=0;editor();}));
        page.addView(Ui.title(this,"我的闹钟",20));
        if(alarms.isEmpty())page.addView(Ui.text(this,"还没有闹钟。添加后，也能在桌面小组件看到它。",14,Ui.MUTED));
        for(Alarm a:alarms){
            LinearLayout card=Ui.card(this,page,android.graphics.Color.WHITE);LinearLayout row=Ui.row(this);card.addView(row);
            LinearLayout info=Ui.column(this);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            info.addView(Ui.title(this,a.time(),36));info.addView(Ui.text(this,a.label+" · "+a.repeatName(),13,Ui.MUTED));
            Switch toggle=new Switch(this);toggle.setContentDescription(a.time()+" 闹钟开关");toggle.setChecked(a.enabled);row.addView(toggle);
            toggle.setOnCheckedChangeListener((b,on)->{a.enabled=on;Scheduler.save(this,a);home();if(on&&!Scheduler.permitted(this))guide();});
            card.addView(Ui.text(this,a.modeName()+"  /  "+a.sourceDescription(),13,Ui.GREEN));
            if(a.snoozeAt>0)card.addView(Ui.text(this,"稍后提醒："+new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(a.snoozeAt)),13,Ui.GREEN));
            info.setOnClickListener(v->{editing=Store.get(this,a.id);tab=0;editor();});
            card.setOnClickListener(v->{editing=Store.get(this,a.id);tab=0;editor();});
        }
        Button widget=Ui.button(this,AlarmWidget.hasWidget(this)?"✓ 闹钟表已添加到桌面":"将我的闹钟表添加到桌面",false,this::pinWidget);
        widget.setEnabled(!AlarmWidget.hasWidget(this));page.addView(widget);
        String status=Store.prefs(this).getString("status","");if(!status.isEmpty())page.addView(Ui.text(this,status,12,Ui.MUTED));
        page.addView(Ui.text(this,"青铃 1.2.0  ·  为你的每一个明天",12,Ui.MUTED));
    }
    private void editor(){
        tab=0;final Alarm a=editing;LinearLayout page=Ui.page(this);
        page.addView(Ui.text(this,"青铃 · 设定闹钟",12,Ui.GREEN));
        page.addView(Ui.title(this,Store.get(this,a.id)==null?"新的唤醒约定":"编辑唤醒约定",28));
        LinearLayout timeCard=Ui.card(this,page,Ui.PALE);
        TimePicker time=(TimePicker)getLayoutInflater().inflate(R.layout.time_picker,timeCard,false);time.setIs24HourView(true);time.setHour(a.hour);time.setMinute(a.minute);
        time.setOnTimeChangedListener((v,h,m)->{a.hour=h;a.minute=m;});timeCard.addView(time);
        page.addView(Ui.title(this,"给这个提醒起个名字",17));
        EditText label=new EditText(this);label.setSingleLine(true);label.setText(a.label);label.setHint("例如：上班、午休、出发");label.setTextColor(Ui.INK);
        label.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});page.addView(label);
        label.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){a.label=s.toString();}public void afterTextChanged(android.text.Editable e){}});
        Button repeat=Ui.button(this,"重复 · "+a.repeatName(),false,()->{});
        repeat.setOnClickListener(v->{boolean[] selected=new boolean[7];for(int n=0;n<7;n++)selected[n]=(a.days&(1<<n))!=0;
            new AlertDialog.Builder(this).setTitle("重复日期（不选为仅一次）").setMultiChoiceItems(new String[]{"周一","周二","周三","周四","周五","周六","周日"},selected,(d,which,on)->selected[which]=on)
                .setPositiveButton("确定",(d,w)->{a.days=0;for(int n=0;n<7;n++)if(selected[n])a.days|=1<<n;repeat.setText(getString(R.string.repeat_label,a.repeatName()));}).setNegativeButton("取消",null).show();});page.addView(repeat);
        page.addView(Ui.title(this,"唤醒方式",18));
        RadioGroup modes=new RadioGroup(this);String[] names={"纯音乐","纯振动","音乐 + 振动"};
        for(int n=0;n<3;n++){RadioButton r=new RadioButton(this);r.setText(names[n]);r.setTextColor(Ui.INK);r.setId(100+n);r.setMinHeight(Ui.dp(this,48));modes.addView(r);}modes.check(100+a.mode);
        modes.setOnCheckedChangeListener((g,id)->a.mode=id-100);page.addView(modes);
        LinearLayout source=Ui.card(this,page,android.graphics.Color.WHITE);source.addView(Ui.title(this,"音乐来源",18));
        sourceView=Ui.text(this,a.sourceDescription(),14,Ui.GREEN);source.addView(sourceView);
        source.addView(Ui.button(this,"导入本地音乐",false,()->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try{startActivityForResult(intent,42);}catch(ActivityNotFoundException e){message("无法打开文件选择器","请安装或启用系统文件管理器。");}
        }));
        source.addView(Ui.button(this,"去网易云 · 从歌单选歌",false,this::openNetEase));
        source.addView(Ui.button(this,"接收分享链接 / 检查可导入歌曲",false,this::playlistDialog));
        source.addView(Ui.button(this,"使用内置 · 晨间微光",false,()->{a.tracks.clear();a.trackTitles.clear();a.source="内置 · 晨间微光";a.playlist="";sourceView.setText(a.sourceDescription());}));
        source.addView(Ui.text(this,"在网易云的歌单里选中歌曲，点“分享 → 更多 → 青铃”；也可复制歌曲或歌单链接后返回粘贴。青铃会标出不可导入曲目。\n本地支持 MP3 / M4A / FLAC / WAV，不支持 NCM。导入后离线循环播放。",12,Ui.MUTED));
        page.addView(Ui.title(this,"播放强度",18));
        TextView volume=Ui.text(this,a.volume+"% × 系统媒体音量",14,Ui.MUTED);page.addView(volume);
        SeekBar seek=new SeekBar(this);seek.setMax(100);seek.setMin(10);seek.setProgress(a.volume);page.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int v,boolean u){a.volume=v;volume.setText(getString(R.string.volume_label,v));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        page.addView(Ui.text(this,"使用媒体通道，跟随系统选定的有线／蓝牙耳机。请先连接耳机并试听，勿将媒体音量设为 0。耳机断开时改为振动。",13,Ui.MUTED));
        outputView=Ui.text(this,AudioOutput.available(this),13,Ui.GREEN);page.addView(outputView);
        page.addView(Ui.button(this,"▷  试听 30 秒",false,()->{
            if(AlarmService.activeId>0){toast("当前闹钟正在响铃，请先停止");return;}
            startForegroundService(new Intent(this,AlarmService.class).putExtra("id",-1).putExtra("preview",a.json().toString()));
            toast("正在试听，可用音量键调整媒体音量");
        }));
        page.addView(Ui.button(this,"停止试听",false,()->AlarmService.dismiss(this,-1,false)));
        page.addView(Ui.button(this,"保存闹钟",true,()->{saveEditor();tab=1;home();}));
        if(Store.get(this,a.id)!=null)page.addView(Ui.button(this,"删除闹钟",false,()->new AlertDialog.Builder(this).setTitle("删除这个闹钟？").setMessage(a.time()+" · "+a.label)
            .setPositiveButton("删除",(d,w)->{Scheduler.cancel(this,a);AlarmService.dismiss(this,a.id,false);Store.delete(this,a.id);AlarmWidget.update(this);editing=null;persistDraft();tab=1;home();}).setNegativeButton("取消",null).show()));
    }
    private void saveEditor(){
        Alarm a=editing;if(a==null)return;
        if(a.label.trim().isEmpty())a.label="给自己的提醒";
        AlarmService.dismiss(this,-1,false);a.enabled=true;Scheduler.save(this,a);editing=null;persistDraft();
        toast(Scheduler.permitted(this)?"已保存 "+a.time():"已保存，请到系统设置开启精确闹钟权限");
    }
    @Override public void onBackPressed(){if(tab!=1){tab=1;home();}else super.onBackPressed();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=42||result!=RESULT_OK||data==null||data.getData()==null||editing==null)return;
        Uri uri=data.getData();String name="本地音乐";
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}
        String title=name;Alarm a=editing;ProgressDialog progress=progress("正在导入并验证音乐…");
        worker.execute(()->{try{String path=MusicImport.local(this,uri);ui(()->{progress.dismiss();a.tracks.clear();a.trackTitles.clear();a.tracks.add(path);a.source="本地 · "+title;a.playlist="";persistDraft();if(editing==a)sourceView.setText(a.sourceDescription());toast("导入成功，保存闹钟后生效");});}catch(Exception e){ui(()->{progress.dismiss();message("导入失败",e.getMessage());});}});
    }
    private ProgressDialog progress(String title){ProgressDialog p=new ProgressDialog(this);p.setMessage(title);p.setCancelable(false);p.show();return p;}
    private void openNetEase(){
        persistDraft();
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.netease.cloudmusic");
        if(launch==null){message("未找到网易云音乐","请先安装网易云音乐，或通过分享链接选择歌曲。");return;}
        if(editing!=null&&!editing.playlist.isEmpty())try{
            NetEaseLink link=NetEaseLink.parse(editing.playlist);
            launch=new Intent(Intent.ACTION_VIEW,Uri.parse("orpheus://"+link.kind+"/"+link.id)).setPackage("com.netease.cloudmusic");
        }catch(Exception ignored){}
        try{startActivity(launch);toast("从歌单选歌后，分享 → 更多 → 青铃");}
        catch(Exception e){Intent fallback=getPackageManager().getLaunchIntentForPackage("com.netease.cloudmusic");if(fallback!=null)startActivity(fallback);}
    }
    private void playlistDialog(){
        EditText input=new EditText(this);input.setHint("网易云歌曲 / 歌单分享链接");input.setText(editing.playlist);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);input.setMaxLines(4);
        new AlertDialog.Builder(this).setTitle("从网易云选歌").setMessage("粘贴分享链接。下一页会检查可导入状态，VIP、付费或无公开音频的歌曲标红且不可勾选。")
            .setView(input).setPositiveButton("检查并选歌",(d,w)->loadShared(input.getText().toString())).setNegativeButton("取消",null).show();
    }
    private void loadShared(String text){
        if(editing==null)return;
        Alarm a=editing;ProgressDialog p=progress("正在读取网易云分享…");
        worker.execute(()->{try{
            MusicImport.Playlist list=MusicImport.shared(text);
            ui(()->{p.dismiss();SongPicker.show(this,list,selected->download(a,text,list.title,selected));});
        }catch(Exception e){ui(()->{p.dismiss();message("无法读取分享",e.getMessage());});}});
    }
    private void download(Alarm a,String id,String title,List<MusicImport.Song> songs){
        ProgressDialog p=progress("准备缓存歌曲…");
        worker.execute(()->{
            List<String> files=new ArrayList<>(),failed=new ArrayList<>(),titles=new ArrayList<>();int count=0;
            for(MusicImport.Song s:songs){if(Thread.currentThread().isInterrupted())break;String msg="正在缓存 "+(++count)+" / "+songs.size()+"\n"+s.title;ui(()->p.setMessage(msg));
                try{files.add(MusicImport.cache(this,s));titles.add(s.title);}catch(Exception e){failed.add(s.title+"："+e.getMessage());}}
            ui(()->{p.dismiss();if(!files.isEmpty()){a.tracks.clear();a.tracks.addAll(files);a.trackTitles.clear();a.trackTitles.addAll(titles);a.source="网易云 · "+title+"（"+files.size()+" 首已缓存）";a.playlist=id;persistDraft();if(editing==a)sourceView.setText(a.sourceDescription());}
                String report=files.isEmpty()?"没有成功缓存的歌曲，原音乐来源保持不变。":"已缓存 "+files.size()+" 首，保存闹钟后生效。断网时仍可播放。";
                if(!failed.isEmpty())report+="\n\n以下曲目无法导入：\n"+String.join("\n",failed);
                message("歌单导入结果",report);
            });
        });
    }
    private boolean fullScreenAllowed(){return Build.VERSION.SDK_INT<34||getSystemService(NotificationManager.class).canUseFullScreenIntent();}
    private void open(Intent i){try{startActivity(i);}catch(Exception e){toast("此系统未提供该入口，请在系统设置 → 应用 → 青铃中手动设置");}}
    private void guide(){selectTab(2);}
    public void showSettings(){tab=2;settings();}
    private void settings(){tab=2;outputView=null;SettingsPanel.populate(this,Ui.page(this));}
    private void pinWidget(){
        if(AlarmWidget.hasWidget(this)){toast("我的闹钟表已添加到桌面");home();return;}
        long pending=Store.prefs(this).getLong("widgetPinPending",0);
        if(System.currentTimeMillis()-pending<30_000){toast("请完成桌面的添加确认");return;}
        AppWidgetManager m=getSystemService(AppWidgetManager.class);
        if(m.isRequestPinAppWidgetSupported()){
            PendingIntent callback=PendingIntent.getBroadcast(this,100,new Intent(this,AlarmWidget.class).setAction("io.github.ming.alarm.WIDGET_PINNED"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(m.requestPinAppWidget(new ComponentName(this,AlarmWidget.class),null,callback))Store.prefs(this).edit().putLong("widgetPinPending",System.currentTimeMillis()).apply();
        }
        else message("添加桌面小组件","长按桌面空白处 → 添加小部件 → Android 小部件 → 青铃。小组件会显示下一次闹钟，点击即可进入。");
    }
}
