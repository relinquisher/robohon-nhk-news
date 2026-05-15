package com.robohon.nhknews.voiceui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 音声UIのシナリオ登録要求レシーバークラス.
 */
public class RequestScenarioReceiver extends BroadcastReceiver {
    private static final String TAG = RequestScenarioReceiver.class.getSimpleName();

    private static final String ACTION_REQUEST_SCENARIO = "jp.co.sharp.android.voiceui.REQUEST_SCENARIO";
    private static final String KEY_LOCALE = "locale";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REQUEST_SCENARIO.equals(intent.getAction())) {
            Log.d(TAG, "onReceive-S:" + intent.getAction());
            try {
                Intent baseIntent = new Intent();
                baseIntent.putExtra(KEY_LOCALE, intent.getStringExtra(KEY_LOCALE));
                RegisterScenarioService.start(context, baseIntent, RegisterScenarioService.CMD_REQUEST_SCENARIO);
            } catch (IllegalStateException e) {
                // Android 8+: バックグラウンドからのサービス起動制限
                Log.w(TAG, "Cannot start service from background: " + e.getMessage());
            }
            Log.d(TAG, "onReceive-E:" + intent.getAction());
        } else {
            Log.e(TAG, "onReceive Unknown action" + intent.getAction());
        }
    }
}
