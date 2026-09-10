package io.github.ming.alarm;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetEaseLinkTest {
    @Test public void recognizesSharedSong(){
        NetEaseLink link=NetEaseLink.parse("分享歌曲《于是》 https://music.163.com/song?id=1303464858&userid=123");
        assertEquals("song",link.kind);assertEquals("1303464858",link.id);
    }
    @Test public void recognizesPlaylistHashLink(){
        NetEaseLink link=NetEaseLink.parse("https://music.163.com/#/playlist?id=3778678");
        assertEquals("playlist",link.kind);assertEquals("3778678",link.id);
    }
    @Test public void recognizesMobileAndPathLinks(){
        assertEquals("42",NetEaseLink.parse("https://music.163.com/m/song?id=42").id);
        assertEquals("42",NetEaseLink.parse("https://music.163.com/song/42").id);
    }
    @Test public void keepsLegacyPlaylistIds(){assertEquals("playlist",NetEaseLink.parse("3778678").kind);}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnknownHost(){NetEaseLink.parse("https://example.com/song?id=42");}
    @Test(expected=IllegalArgumentException.class) public void rejectsNonMusicShare(){NetEaseLink.parse("random text");}
}
