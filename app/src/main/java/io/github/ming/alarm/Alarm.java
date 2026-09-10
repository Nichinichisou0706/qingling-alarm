package io.github.ming.alarm;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class Alarm {
    public int id, hour = 7, minute = 30, days = 0, mode = 2, volume = 70;
    public boolean enabled = true;
    public String label = "早安，新的一天", source = "内置 · 晨间微光", playlist = "";
    public long nextAt, snoozeAt;
    public final List<String> tracks = new ArrayList<>();
    public JSONObject json() {
        JSONObject j = new JSONObject();
        try {
            j.put("id", id).put("hour", hour).put("minute", minute).put("days", days)
                .put("mode", mode).put("volume", volume).put("enabled", enabled).put("label", label)
                .put("source", source).put("playlist", playlist).put("nextAt", nextAt)
                .put("snoozeAt", snoozeAt).put("tracks", new JSONArray(tracks));
        } catch (Exception e) { throw new IllegalStateException(e); }
        return j;
    }
    public static Alarm from(JSONObject j) {
        Alarm a = new Alarm();
        a.id = j.optInt("id"); a.hour = j.optInt("hour",7); a.minute = j.optInt("minute",30);
        a.days = j.optInt("days"); a.mode = j.optInt("mode",2); a.volume = j.optInt("volume",70);
        a.enabled = j.optBoolean("enabled",true); a.label = j.optString("label", "早安，新的一天");
        a.source = j.optString("source","内置 · 晨间微光"); a.playlist = j.optString("playlist");
        a.nextAt = j.optLong("nextAt"); a.snoozeAt = j.optLong("snoozeAt");
        JSONArray ts = j.optJSONArray("tracks");
        if (ts != null) for (int i=0;i<ts.length();i++) a.tracks.add(ts.optString(i));
        return a;
    }
    public String time() { return String.format(java.util.Locale.CHINA, "%02d:%02d", hour, minute); }
    public String modeName() { return new String[]{"纯音乐", "纯振动", "音乐 + 振动"}[mode]; }
    public String repeatName() {
        if (days == 0) return "仅一次";
        if (days == 127) return "每天";
        if (days == 31) return "工作日";
        StringBuilder b = new StringBuilder();
        for(int i=0;i<7;i++) if((days & (1<<i)) != 0) b.append("周").append("一二三四五六日".charAt(i)).append(" ");
        return b.toString().trim();
    }
}
