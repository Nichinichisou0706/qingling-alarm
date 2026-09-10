package io.github.ming.alarm;

import java.time.*;

/** Wall-clock alarms follow the current local time zone, including DST. Monday is bit zero. */
public final class AlarmTime {
    private AlarmTime() {}
    public static long next(int hour, int minute, int days, long now, ZoneId zone) {
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(zone);
        for (int offset=0; offset<=7; offset++) {
            LocalDate date = current.toLocalDate().plusDays(offset);
            if (days != 0 && (days & (1 << (date.getDayOfWeek().getValue()-1))) == 0) continue;
            ZonedDateTime candidate = date.atTime(hour, minute).atZone(zone);
            if (candidate.toInstant().toEpochMilli() > now) return candidate.toInstant().toEpochMilli();
        }
        throw new IllegalArgumentException("Invalid repeat days");
    }
}
