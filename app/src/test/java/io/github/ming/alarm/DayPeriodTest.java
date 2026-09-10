package io.github.ming.alarm;
import org.junit.Test;
import static org.junit.Assert.*;

public class DayPeriodTest {
    @Test public void boundaries() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.atHour(0));
        assertEquals(DayPeriod.NIGHT, DayPeriod.atHour(4));
        assertEquals(DayPeriod.MORNING, DayPeriod.atHour(5));
        assertEquals(DayPeriod.MORNING, DayPeriod.atHour(10));
        assertEquals(DayPeriod.NOON, DayPeriod.atHour(11));
        assertEquals(DayPeriod.NOON, DayPeriod.atHour(13));
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.atHour(14));
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.atHour(17));
        assertEquals(DayPeriod.EVENING, DayPeriod.atHour(18));
        assertEquals(DayPeriod.EVENING, DayPeriod.atHour(21));
        assertEquals(DayPeriod.NIGHT, DayPeriod.atHour(22));
        assertEquals(DayPeriod.NIGHT, DayPeriod.atHour(23));
    }
    @Test public void rejectsOutOfRange() {
        try{DayPeriod.atHour(24);fail();}catch(IllegalArgumentException expected){}
        try{DayPeriod.atHour(-1);fail();}catch(IllegalArgumentException expected){}
    }
}
