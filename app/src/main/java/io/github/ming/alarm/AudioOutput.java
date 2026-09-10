package io.github.ming.alarm;

import android.content.Context;
import android.media.*;
import android.os.Build;

/** Media devices only: never switch music to the Bluetooth call/SCO channel. */
public final class AudioOutput {
    private AudioOutput() {}

    public static boolean headphones(int type) {
        if (Build.VERSION.SDK_INT >= 31 && type == AudioDeviceInfo.TYPE_BLE_HEADSET) return true;
        if (Build.VERSION.SDK_INT >= 28 && type == AudioDeviceInfo.TYPE_HEARING_AID) return true;
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            || type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            || type == AudioDeviceInfo.TYPE_USB_HEADSET;
    }

    public static AudioDeviceInfo preferred(Context c) {
        AudioDeviceInfo best = null;
        for (AudioDeviceInfo device : c.getSystemService(AudioManager.class).getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (!headphones(device.getType())) continue;
            if (best == null || rank(device) > rank(best)) best = device;
        }
        return best;
    }

    private static int rank(AudioDeviceInfo d) {
        return d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT>=31&&d.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) ? 2 : 1;
    }

    @android.annotation.SuppressLint("SwitchIntDef") // Other endpoints intentionally use the generic label.
    public static String name(AudioDeviceInfo d) {
        if (d == null) return "系统默认媒体输出";
        String kind = switch (d.getType()) {
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙媒体耳机";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机";
            case AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 耳机";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "手机扬声器";
            case AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙 LE 耳机";
            default -> "媒体设备";
        };
        return kind + " · " + d.getProductName();
    }

    public static String available(Context c) {
        AudioDeviceInfo d = preferred(c);
        return d == null ? "未检测到媒体耳机 · 使用系统输出" : "已连接 " + name(d);
    }
}
