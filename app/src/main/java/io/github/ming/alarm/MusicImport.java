package io.github.ming.alarm;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public final class MusicImport {
    public static final class Song {
        public final long id; public final String title;
        Song(long id,String title) {this.id=id;this.title=title;}
    }
    public static final class Playlist {
        public String title; public final List<Song> songs=new ArrayList<>();
    }
    public interface Progress {void update(String text);}
    public static String playlistId(String input) {
        String s=input.trim();
        if(s.matches("[0-9]{1,19}")) return s;
        Matcher m=Pattern.compile("music\\.163\\.com/(?:#/?|m/)?playlist(?:\\?[^\\s]*?\\bid=|/)([0-9]{1,19})(?![0-9])").matcher(s);
        if(m.find()) return m.group(1);
        throw new IllegalArgumentException("请粘贴网易云公开歌单链接或数字歌单 ID（不支持短链接）");
    }
    private static HttpURLConnection connect(String address) throws IOException {
        for(int i=0;i<6;i++) {
            URL u=new URL(address.replaceFirst("^http://","https://"));
            if(!"https".equals(u.getProtocol())) throw new IOException("不支持的下载地址");
            HttpURLConnection c=(HttpURLConnection)u.openConnection();
            c.setConnectTimeout(15_000);c.setReadTimeout(20_000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Mozilla/5.0");c.setRequestProperty("Referer","https://music.163.com/");
            int code=c.getResponseCode();
            if(code>=300 && code<400) {String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("重定向缺少地址");address=new URL(u,location).toString();continue;}
            if(code!=200) {c.disconnect();throw new IOException("网易云返回 HTTP "+code+"，歌曲可能受版权或登录限制");}
            return c;
        }
        throw new IOException("重定向次数过多");
    }
    public static Playlist load(String id) throws Exception {
        HttpURLConnection c=connect("https://music.163.com/api/playlist/detail?id="+playlistId(id));
        String data;
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            copy(in,out,8*1024*1024);data=out.toString(StandardCharsets.UTF_8.name());
        } finally {c.disconnect();}
        JSONObject root=new JSONObject(data);
        JSONObject p=root.optJSONObject("result"); if(p==null)p=root.optJSONObject("playlist");
        if(root.optInt("code")!=200 || p==null) throw new IOException("无法读取公开歌单；请检查 ID、可见性或稍后重试");
        Playlist result=new Playlist();result.title=p.optString("name","网易云歌单");
        JSONArray songs=p.optJSONArray("tracks");
        if(songs!=null) for(int i=0;i<Math.min(songs.length(),500);i++) {
            JSONObject s=songs.getJSONObject(i); JSONArray artists=s.optJSONArray("artists");if(artists==null)artists=s.optJSONArray("ar");
            String artist=artists!=null && artists.length()>0?artists.getJSONObject(0).optString("name"):"";
            result.songs.add(new Song(s.getLong("id"),s.optString("name")+" · "+artist));
        }
        if(result.songs.isEmpty()) throw new IOException("歌单为空或需要登录，暂无法导入");
        return result;
    }
    public static String local(Context c,Uri uri) throws Exception {
        File tmp=File.createTempFile("import-",".part",Store.music(c));
        try {
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)) {
                if(in==null) throw new IOException("无法读取所选文件");copy(in,out,128*1024*1024);
            }
            validate(tmp);return finish(tmp);
        } finally {if(tmp.exists())tmp.delete();}
    }
    public static String cache(Context c,Song song) throws Exception {
        File tmp=File.createTempFile("netease-",".part",Store.music(c));
        HttpURLConnection connection=null;
        try {
            connection=connect("https://music.163.com/song/media/outer/url?id="+song.id+".mp3");
            String type=connection.getContentType();
            if(type!=null && (type.contains("text/") || type.contains("json"))) throw new IOException("该歌曲暂不可直接播放");
            try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(tmp)) {copy(in,out,80*1024*1024);}
            validate(tmp);return finish(tmp);
        } finally {if(connection!=null)connection.disconnect();if(tmp.exists())tmp.delete();}
    }
    private static String finish(File tmp) throws IOException {
        File dest=new File(tmp.getParentFile(),UUID.randomUUID()+".audio");
        if(!tmp.renameTo(dest)) throw new IOException("保存音乐失败");return dest.getName();
    }
    private static void validate(File f) throws Exception {
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String duration=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String hasAudio=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            if(duration==null || Long.parseLong(duration)<1000 || !"yes".equals(hasAudio)) throw new IOException("不是可播放的音频文件（不支持 NCM 加密文件）");
        } finally { r.release(); }
    }
    private static void copy(InputStream in,OutputStream out,int max) throws IOException {
        byte[] buf=new byte[16384];int n,total=0;
        long deadline=System.nanoTime()+120_000_000_000L;
        while((n=in.read(buf))!=-1) {
            if(Thread.currentThread().isInterrupted()) throw new IOException("已取消导入");
            if(System.nanoTime()>deadline) throw new IOException("读取超过 2 分钟，请重试");
            total+=n;if(total>max)throw new IOException("文件过大，请选择较小的音频文件");out.write(buf,0,n);
        }
    }
}
