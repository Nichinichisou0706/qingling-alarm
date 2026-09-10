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
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView sourceView;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){if(editing==null)home();timer.postDelayed(this,30_000);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);
        if(state!=null && state.containsKey("editing")) {
            try {editing=Alarm.from(new org.json.JSONObject(state.getString("editing")));}catch(Exception ignored){}
        }
        Scheduler.restore(this,false);
        if(editing!=null) editor(); else {home(); handleIntent(getIntent());}
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private void handleIntent(Intent i){int id=i.getIntExtra("edit",0);Alarm a=Store.get(this,id);if(a!=null){editing=a;editor();}i.removeExtra("edit");}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(editing!=null)out.putString("editing",editing.json().toString());}
    @Override protected void onResume(){super.onResume();if(editing==null)home();timer.postDelayed(refresh,30_000);}
    @Override protected void onPause(){timer.removeCallbacks(refresh);super.onPause();}
    @Override protected void onDestroy(){worker.shutdownNow();timer.removeCallbacksAndMessages(null);super.onDestroy();}
    private void ui(Runnable r){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())r.run();});}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void message(String title,String text){new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("知道了",null).show();}
    private void home(){
        LinearLayout page=Ui.page(this);
        page.addView(Ui.text(this,"Q I N G L I N G   /   青 铃",12,Ui.GREEN));
        LinearLayout heading=Ui.row(this);page.addView(heading);
        LinearLayout words=Ui.column(this);heading.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        words.addView(Ui.title(this,"把早安，\n交给喜欢的旋律。",27));
        words.addView(Ui.text(this,"音乐相伴 · 轻轻唤醒",13,Ui.MUTED));
        heading.addView(new MascotView(this),new LinearLayout.LayoutParams(Ui.dp(this,116),Ui.dp(this,136)));
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
        if(!Scheduler.permitted(this)||!getSystemService(NotificationManager.class).areNotificationsEnabled()||!fullScreenAllowed())
            page.addView(Ui.button(this,"完成权限设置，保证准时唤醒  →",false,this::guide));
        page.addView(Ui.button(this,"＋  添加闹钟",true,()->{editing=new Alarm();editing.id=Store.newId(this);editor();}));
        page.addView(Ui.title(this,"我的闹钟",20));
        if(alarms.isEmpty())page.addView(Ui.text(this,"还没有闹钟。添加后，也能在桌面小组件看到它。",14,Ui.MUTED));
        for(Alarm a:alarms){
            LinearLayout card=Ui.card(this,page,android.graphics.Color.WHITE);LinearLayout row=Ui.row(this);card.addView(row);
            LinearLayout info=Ui.column(this);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            info.addView(Ui.title(this,a.time(),36));info.addView(Ui.text(this,a.label+" · "+a.repeatName(),13,Ui.MUTED));
            Switch toggle=new Switch(this);toggle.setContentDescription(a.time()+" 闹钟开关");toggle.setChecked(a.enabled);row.addView(toggle);
            toggle.setOnCheckedChangeListener((b,on)->{a.enabled=on;Scheduler.save(this,a);home();if(on&&!Scheduler.permitted(this))guide();});
            card.addView(Ui.text(this,a.modeName()+"  /  "+a.source,13,Ui.GREEN));
            if(a.snoozeAt>0)card.addView(Ui.text(this,"稍后提醒："+new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(a.snoozeAt)),13,Ui.GREEN));
            info.setOnClickListener(v->{editing=Store.get(this,a.id);editor();});
            card.setOnClickListener(v->{editing=Store.get(this,a.id);editor();});
        }
        page.addView(Ui.button(this,"添加到桌面",false,this::pinWidget));
        page.addView(Ui.button(this,"HyperOS 设置与使用说明",false,this::guide));
        String status=Store.prefs(this).getString("status","");if(!status.isEmpty())page.addView(Ui.text(this,status,12,Ui.MUTED));
        page.addView(Ui.text(this,"青铃 1.0.0  ·  为你的每一个明天",12,Ui.MUTED));
    }
    private void editor(){
        final Alarm a=editing;LinearLayout page=Ui.page(this);
        page.addView(Ui.button(this,"‹  返回",false,this::leaveEditor));
        page.addView(Ui.text(this,"MAKE ROOM FOR MORNING",12,Ui.GREEN));
        page.addView(Ui.title(this,Store.get(this,a.id)==null?"新的唤醒约定":"编辑唤醒约定",28));
        LinearLayout timeCard=Ui.card(this,page,Ui.PALE);
        TimePicker time=(TimePicker)getLayoutInflater().inflate(R.layout.time_picker,timeCard,false);time.setIs24HourView(true);time.setHour(a.hour);time.setMinute(a.minute);
        time.setOnTimeChangedListener((v,h,m)->{a.hour=h;a.minute=m;});timeCard.addView(time);
        page.addView(Ui.title(this,"给早晨起个名字",17));
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
        sourceView=Ui.text(this,a.source,14,Ui.GREEN);source.addView(sourceView);
        source.addView(Ui.button(this,"导入本地音乐",false,()->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try{startActivityForResult(intent,42);}catch(ActivityNotFoundException e){message("无法打开文件选择器","请安装或启用系统文件管理器。");}
        }));
        source.addView(Ui.button(this,"导入网易云歌单",false,this::playlistDialog));
        source.addView(Ui.button(this,"使用内置 · 晨间微光",false,()->{a.tracks.clear();a.source="内置 · 晨间微光";a.playlist="";sourceView.setText(a.source);}));
        source.addView(Ui.text(this,"本地支持 MP3 / M4A / FLAC / WAV 等系统可解码音频；不支持加密 NCM。歌单曲目缓存后按顺序循环，断网可用。纯振动模式不播放音乐。",12,Ui.MUTED));
        page.addView(Ui.title(this,"播放强度",18));
        TextView volume=Ui.text(this,a.volume+"% × 系统媒体音量",14,Ui.MUTED);page.addView(volume);
        SeekBar seek=new SeekBar(this);seek.setMax(100);seek.setMin(10);seek.setProgress(a.volume);page.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int v,boolean u){a.volume=v;volume.setText(getString(R.string.volume_label,v));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        page.addView(Ui.text(this,"使用媒体通道，跟随系统选定的有线／蓝牙耳机。请先连接耳机并试听，勿将媒体音量设为 0。耳机断开时改为振动。",13,Ui.MUTED));
        page.addView(Ui.button(this,"▷  试听 30 秒",false,()->{
            if(AlarmService.activeId>0){toast("当前闹钟正在响铃，请先停止");return;}
            startForegroundService(new Intent(this,AlarmService.class).putExtra("id",-1).putExtra("preview",a.json().toString()));
            toast("正在试听，可用音量键调整媒体音量");
        }));
        page.addView(Ui.button(this,"停止试听",false,()->AlarmService.dismiss(this,-1,false)));
        page.addView(Ui.button(this,"保存闹钟",true,()->{
            if(a.label.trim().isEmpty())a.label="早安，新的一天";
            AlarmService.dismiss(this,-1,false);a.enabled=true;Scheduler.save(this,a);editing=null;home();
            if(!Scheduler.permitted(this))guide();else toast("已设置 "+a.time()+" · "+a.repeatName());
        }));
        if(Store.get(this,a.id)!=null)page.addView(Ui.button(this,"删除闹钟",false,()->new AlertDialog.Builder(this).setTitle("删除这个闹钟？").setMessage(a.time()+" · "+a.label)
            .setPositiveButton("删除",(d,w)->{Scheduler.cancel(this,a);AlarmService.dismiss(this,a.id,false);Store.delete(this,a.id);AlarmWidget.update(this);editing=null;home();}).setNegativeButton("取消",null).show()));
    }
    private void leaveEditor(){new AlertDialog.Builder(this).setTitle("放弃尚未保存的修改？").setPositiveButton("放弃",(d,w)->{AlarmService.dismiss(this,-1,false);editing=null;home();}).setNegativeButton("继续编辑",null).show();}
    @Override public void onBackPressed(){if(editing!=null)leaveEditor();else super.onBackPressed();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=42||result!=RESULT_OK||data==null||data.getData()==null||editing==null)return;
        Uri uri=data.getData();String name="本地音乐";
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}
        String title=name;Alarm a=editing;ProgressDialog progress=progress("正在导入并验证音乐…");
        worker.execute(()->{try{String path=MusicImport.local(this,uri);ui(()->{progress.dismiss();a.tracks.clear();a.tracks.add(path);a.source="本地 · "+title;a.playlist="";if(editing==a)sourceView.setText(a.source);toast("导入成功，保存闹钟后生效");});}catch(Exception e){ui(()->{progress.dismiss();message("导入失败",e.getMessage());});}});
    }
    private ProgressDialog progress(String title){ProgressDialog p=new ProgressDialog(this);p.setMessage(title);p.setCancelable(false);p.show();return p;}
    private void playlistDialog(){
        EditText input=new EditText(this);input.setHint("网易云歌单分享链接 / 歌单 ID");input.setText(editing.playlist);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);input.setMaxLines(4);
        new AlertDialog.Builder(this).setTitle("网易云公开歌单").setMessage("读取公开歌单，选择最多 20 首可直接播放的曲目并缓存。不支持登录、VIP、付费或受限歌曲；接口变化时可改用本地导入。歌单不会自动同步。")
            .setView(input).setPositiveButton("读取歌单",(d,w)->loadPlaylist(input.getText().toString())).setNegativeButton("取消",null).show();
    }
    private void loadPlaylist(String text){
        final String id;try{id=MusicImport.playlistId(text);}catch(Exception e){message("链接无法识别",e.getMessage());return;}
        Alarm a=editing;ProgressDialog p=progress("正在读取公开歌单…");
        worker.execute(()->{try{MusicImport.Playlist list=MusicImport.load(id);ui(()->{p.dismiss();chooseSongs(a,id,list);});}catch(Exception e){ui(()->{p.dismiss();message("读取失败",e.getMessage());});}});
    }
    private void chooseSongs(Alarm a,String id,MusicImport.Playlist list){
        String[] titles=new String[list.songs.size()];boolean[] checked=new boolean[titles.length];
        for(int i=0;i<titles.length;i++)titles[i]=list.songs.get(i).title;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(list.title+" · 选择 1–20 首")
            .setMultiChoiceItems(titles,checked,(d,which,on)->checked[which]=on).setPositiveButton("缓存所选歌曲",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            List<MusicImport.Song> selected=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])selected.add(list.songs.get(i));
            if(selected.isEmpty()||selected.size()>20){toast("请选择 1–20 首歌曲");return;}dialog.dismiss();download(a,id,list.title,selected);
        }));dialog.show();
    }
    private void download(Alarm a,String id,String title,List<MusicImport.Song> songs){
        ProgressDialog p=progress("准备缓存歌曲…");
        worker.execute(()->{
            List<String> files=new ArrayList<>(),failed=new ArrayList<>();int count=0;
            for(MusicImport.Song s:songs){if(Thread.currentThread().isInterrupted())break;String msg="正在缓存 "+(++count)+" / "+songs.size()+"\n"+s.title;ui(()->p.setMessage(msg));
                try{files.add(MusicImport.cache(this,s));}catch(Exception e){failed.add(s.title+"："+e.getMessage());}}
            ui(()->{p.dismiss();if(!files.isEmpty()){a.tracks.clear();a.tracks.addAll(files);a.source="网易云 · "+title+"（"+files.size()+" 首已缓存）";a.playlist=id;if(editing==a)sourceView.setText(a.source);}
                String report=files.isEmpty()?"没有成功缓存的歌曲，原音乐来源保持不变。":"已缓存 "+files.size()+" 首，保存闹钟后生效。断网时仍可播放。";
                if(!failed.isEmpty())report+="\n\n以下曲目无法导入：\n"+String.join("\n",failed);
                message("歌单导入结果",report);
            });
        });
    }
    private boolean fullScreenAllowed(){return Build.VERSION.SDK_INT<34||getSystemService(NotificationManager.class).canUseFullScreenIntent();}
    private void open(Intent i){try{startActivity(i);}catch(Exception e){toast("此系统未提供该入口，请在系统设置 → 应用 → 青铃中手动设置");}}
    private void guide(){
        LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,22),Ui.dp(this,8),Ui.dp(this,22),Ui.dp(this,18));ScrollView scroll=new ScrollView(this);scroll.addView(content);
        content.addView(Ui.text(this,"适配目标：Xiaomi HyperOS 3.0.10.0.VMICNXM（Android 15）。不同 HyperOS 版本的设置名称可能不同。",14,Ui.INK));
        content.addView(Ui.button(this,(Scheduler.permitted(this)?"✓ ":"○ ")+"允许精确闹钟",false,()->{if(Build.VERSION.SDK_INT>=31)open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}));
        content.addView(Ui.button(this,(getSystemService(NotificationManager.class).areNotificationsEnabled()?"✓ ":"○ ")+"允许通知与锁屏通知",false,()->open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()))));
        content.addView(Ui.button(this,(fullScreenAllowed()?"✓ ":"○ ")+"允许全屏提醒",false,()->{if(Build.VERSION.SDK_INT>=34)open(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));}));
        content.addView(Ui.button(this,"打开应用详情 · 设置后台权限",false,()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))));
        content.addView(Ui.button(this,"打开 HyperOS 自启动管理",false,()->open(new Intent().setComponent(new ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity")))));
        content.addView(Ui.text(this,"① 开启青铃的后台自启动。\n② 应用省电策略选“无限制”，在最近任务中锁定青铃。\n③ 在其他权限中允许锁屏显示、后台弹出界面（如果系统提供）。\n④ 允许通知，开启锁屏通知、悬浮通知和全屏提醒。\n⑤ 连上耳机，调高媒体音量，并设置一个 2 分钟后的闹钟，锁屏验证。\n\n媒体播放跟随系统当前音频路由；通话、勿扰和蓝牙断连可能影响提醒。耳机断开或音频不可用时会改为振动。\n\n手机关机、强行停止应用、撤销精确闹钟权限时不能正常响铃；强行停止后需重新打开青铃。重启会恢复计划，10 分钟内错过的闹钟将补响；更早的按下一次时间计算。\n\n响铃最多 10 分钟，稍后提醒为 5 分钟。重新编辑或关闭闹钟会取消其稍后提醒。",14,Ui.MUTED));
        content.addView(Ui.button(this,"清理未使用的音乐缓存",false,()->{
            Set<String> used=new HashSet<>();for(Alarm a:Store.all(this))used.addAll(a.tracks);if(editing!=null)used.addAll(editing.tracks);
            long bytes=0;File[] files=Store.music(this).listFiles();if(files!=null)for(File f:files)if(!used.contains(f.getName())&&!f.getName().endsWith(".part")){long n=f.length();if(f.delete())bytes+=n;}
            toast("已清理 "+(bytes/1024/1024)+" MB 未使用缓存");
        }));
        new AlertDialog.Builder(this).setTitle("让青铃准时抵达").setView(scroll).setPositiveButton("完成",(d,w)->{Scheduler.restore(this,false);if(editing==null)home();}).show();
    }
    private void pinWidget(){
        AppWidgetManager m=getSystemService(AppWidgetManager.class);
        if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,AlarmWidget.class),null,null);
        else message("添加桌面小组件","长按桌面空白处 → 添加小部件 → Android 小部件 → 青铃。小组件会显示下一次闹钟，点击即可进入。");
    }
}
