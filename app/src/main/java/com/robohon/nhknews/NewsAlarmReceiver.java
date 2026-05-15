package com.robohon.nhknews;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * AlarmManagerからのブロードキャストを受け取り、ニュース読み上げActivityを起動する.
 * 静寂時間帯（デフォルト23:00〜07:00）は読み上げをスキップする.
 */
public class NewsAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = NewsAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isQuietHours(context)) {
            Log.d(TAG, "Quiet hours - skipping news read");
            return;
        }

        Log.d(TAG, "Alarm received, launching NewsReadActivity");

        Intent newsIntent = new Intent(context, NewsReadActivity.class);
        newsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newsIntent);
    }

    /**
     * 現在が静寂時間帯かどうかを判定する.
     */
    public static boolean isQuietHours(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("nhknews", Context.MODE_PRIVATE);
        int quietStart = prefs.getInt("quiet_start_hour", 23);
        int quietStartMin = prefs.getInt("quiet_start_min", 0);
        int quietEnd = prefs.getInt("quiet_end_hour", 7);
        int quietEndMin = prefs.getInt("quiet_end_min", 0);

        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int startMinutes = quietStart * 60 + quietStartMin;
        int endMinutes = quietEnd * 60 + quietEndMin;

        if (startMinutes <= endMinutes) {
            // 例: 01:00〜06:00（同日内）
            return nowMinutes >= startMinutes && nowMinutes < endMinutes;
        } else {
            // 例: 23:00〜07:00（日をまたぐ）
            return nowMinutes >= startMinutes || nowMinutes < endMinutes;
        }
    }
}
