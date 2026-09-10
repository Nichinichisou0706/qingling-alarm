package io.github.ming.alarm;
import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class AlarmTimeTest {
    private long at(String iso) { return ZonedDateTime.parse(iso).toInstant().toEpochMilli(); }
    @Test public void oneShotTodayAndTomorrow() {
        ZoneId z=ZoneId.of("Asia/Shanghai");
        assertEquals(at("2026-09-10T07:30+08:00"),AlarmTime.next(7,30,0,at("2026-09-10T07:29+08:00"),z));
        assertEquals(at("2026-09-11T07:30+08:00"),AlarmTime.next(7,30,0,at("2026-09-10T07:30+08:00"),z));
    }
    @Test public void weekdaysSkipWeekend() {
        assertEquals(at("2026-09-14T07:30+08:00"),AlarmTime.next(7,30,31,at("2026-09-11T08:00+08:00"),ZoneId.of("Asia/Shanghai")));
    }
    @Test public void singleDayWrapsFullWeek() {
        assertEquals(at("2026-09-17T07:30+08:00"),AlarmTime.next(7,30,8,at("2026-09-10T07:30+08:00"),ZoneId.of("Asia/Shanghai")));
    }
    @Test public void springGapMovesToValidTime() {
        assertEquals(at("2026-03-08T03:30-04:00"),AlarmTime.next(2,30,127,at("2026-03-08T00:00-05:00"),ZoneId.of("America/New_York")));
    }
    @Test public void fallOverlapRingsOnce() {
        assertEquals(at("2026-11-02T01:30-05:00"),AlarmTime.next(1,30,127,at("2026-11-01T01:40-04:00"),ZoneId.of("America/New_York")));
    }
    @Test public void lateNightRollsOverYear() {
        assertEquals(at("2027-01-01T00:00+08:00"),AlarmTime.next(0,0,0,at("2026-12-31T23:59+08:00"),ZoneId.of("Asia/Shanghai")));
    }
}
