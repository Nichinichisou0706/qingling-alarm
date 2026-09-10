package io.github.ming.alarm;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistInputTest {
    @Test public void acceptsIdsAndSharedPlaylistLinks() {
        assertEquals("3778678",MusicImport.playlistId("3778678"));
        assertEquals("3778678",MusicImport.playlistId("分享歌单 https://music.163.com/#/playlist?id=3778678&userid=1"));
        assertEquals("123",MusicImport.playlistId("https://music.163.com/m/playlist?id=123"));
        assertEquals("123",MusicImport.playlistId("https://music.163.com/playlist/123"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsSongLinks(){MusicImport.playlistId("https://music.163.com/song?id=123");}
    @Test(expected=IllegalArgumentException.class) public void rejectsArbitraryUrls(){MusicImport.playlistId("https://example.com/playlist?id=123");}
    @Test(expected=IllegalArgumentException.class) public void rejectsEmpty(){MusicImport.playlistId(" ");}
}
