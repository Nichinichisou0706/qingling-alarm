package io.github.ming.alarm;
import org.junit.Test;
import static org.junit.Assert.*;

public class AlarmSourceTest {
    @Test public void listsSelectedTitles() {
        Alarm a=new Alarm();a.playlist="song/1303464858";a.tracks.add("/cache/1.mp3");a.trackTitles.add("于是");
        assertTrue(a.sourceDescription().contains("于是"));
    }
    @Test public void legacyWithoutTitlesKeepsAudio() {
        Alarm a=new Alarm();a.playlist="song/1";a.tracks.add("/cache/old.mp3");
        assertTrue(a.sourceDescription().contains("已缓存曲目"));
    }
    @Test public void builtInHasNoTrackList() {
        Alarm a=new Alarm();a.source="内置 · 晨间微光";a.tracks.add("/cache/1.mp3");
        assertEquals("内置 · 晨间微光",a.sourceDescription());
    }
}
