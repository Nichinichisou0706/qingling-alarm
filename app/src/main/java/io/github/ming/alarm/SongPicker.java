package io.github.ming.alarm;

import android.app.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class SongPicker {
    public interface Selected {void accept(List<MusicImport.Song> songs);}
    public static void show(Activity a,MusicImport.Playlist playlist,Selected selected){
        List<MusicImport.Song> songs=playlist.songs;
        boolean[] checked=new boolean[songs.size()];
        ExecutorService pool=Executors.newFixedThreadPool(3);
        ListView view=new ListView(a);view.setDividerHeight(1);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(playlist.title)
            .setMessage("选择 1–20 首。红色曲目不可导入；滚动时自动检查可见歌曲。")
            .setView(view).setPositiveButton("导入选中歌曲",null).setNegativeButton("取消",null).create();
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return songs.size();}
            public Object getItem(int p){return songs.get(p);}
            public long getItemId(int p){return songs.get(p).id;}
            public View getView(int position,View convert,ViewGroup parent){
                MusicImport.Song song=songs.get(position);
                LinearLayout row=Ui.row(a);row.setPadding(Ui.dp(a,18),Ui.dp(a,8),Ui.dp(a,12),Ui.dp(a,8));
                LinearLayout text=Ui.column(a);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
                text.addView(Ui.text(a,song.title,15,Ui.INK));
                text.addView(Ui.text(a,song.availabilityText,12,song.availability==2?Ui.GREEN:song.availability>=3?Ui.RED:Ui.MUTED));
                CheckBox box=new CheckBox(a);box.setChecked(checked[position]);box.setEnabled(song.availability==2);box.setClickable(false);box.setFocusable(false);row.addView(box);
                if(song.availability==0&&!pool.isShutdown()){
                    song.availability=1;
                    pool.execute(()->{MusicImport.probe(song);a.runOnUiThread(()->{if(dialog.isShowing()&&!a.isDestroyed())notifyDataSetChanged();});});
                }
                return row;
            }
        };
        view.setAdapter(adapter);
        view.setOnItemClickListener((parent,row,position,id)->{
            MusicImport.Song song=songs.get(position);
            if(song.availability==2){int count=0;for(boolean on:checked)if(on)count++;if(!checked[position]&&count>=20){Toast.makeText(a,"最多选择 20 首",Toast.LENGTH_SHORT).show();return;}checked[position]=!checked[position];}
            else if(song.availability==4)song.availability=0;
            else Toast.makeText(a,song.availabilityText,Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
        });
        dialog.setOnShowListener(d->{
            view.getLayoutParams().height=Ui.dp(a,360);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                List<MusicImport.Song> chosen=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i]&&songs.get(i).availability==2)chosen.add(songs.get(i));
                if(chosen.isEmpty()){Toast.makeText(a,"请选择已检查可导入的歌曲",Toast.LENGTH_LONG).show();return;}
                dialog.dismiss();selected.accept(chosen);
            });
        });
        dialog.setOnDismissListener(d->pool.shutdownNow());dialog.show();
    }
}
