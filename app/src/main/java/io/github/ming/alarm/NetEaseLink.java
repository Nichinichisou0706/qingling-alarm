package io.github.ming.alarm;
import java.util.regex.*;

public final class NetEaseLink {
    public final String kind,id;
    private NetEaseLink(String kind,String id){this.kind=kind;this.id=id;}
    public static NetEaseLink parse(String text){
        String s=text.trim();if(s.matches("[0-9]{1,19}"))return new NetEaseLink("playlist",s);
        Matcher m=Pattern.compile("music\\.163\\.com/(?:#/?|m/)?(playlist|song)(?:\\?[^\\s]*?\\bid=|/)([0-9]{1,19})(?![0-9])").matcher(s);
        if(m.find())return new NetEaseLink(m.group(1),m.group(2));
        throw new IllegalArgumentException("请分享网易云歌曲或歌单链接到青铃");
    }
}
