package com.robohon.nhknews;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toolbar;

import com.robohon.nhknews.voiceui.RegisterScenarioService;

/**
 * NHKニュース読み上げアプリのメインActivity.
 * アラームの設定/解除と状態表示、静寂時間帯の設定を行う.
 * 起動時に即座に1回ニュース取得＋読み上げも実行する.
 */
public class MainActivity extends Activity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final long ALARM_INTERVAL_MS = 60 * 60 * 1000; // 1時間

    private TextView mStatusTextView;
    private Button mBtnQuietStart;
    private Button mBtnQuietEnd;

    private int mQuietStartHour;
    private int mQuietStartMin;
    private int mQuietEndHour;
    private int mQuietEndMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        mStatusTextView = (TextView) findViewById(R.id.status_text);
        mBtnQuietStart = (Button) findViewById(R.id.btn_quiet_start);
        mBtnQuietEnd = (Button) findViewById(R.id.btn_quiet_end);

        // 静寂時間を読み込み
        SharedPreferences prefs = getSharedPreferences("nhknews", MODE_PRIVATE);
        mQuietStartHour = prefs.getInt("quiet_start_hour", 23);
        mQuietStartMin = prefs.getInt("quiet_start_min", 0);
        mQuietEndHour = prefs.getInt("quiet_end_hour", 7);
        mQuietEndMin = prefs.getInt("quiet_end_min", 0);

        updateQuietButtons();

        // 開始時間ボタン
        mBtnQuietStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mQuietStartHour = hourOfDay;
                        mQuietStartMin = minute;
                        saveQuietHours();
                        updateQuietButtons();
                    }
                }, mQuietStartHour, mQuietStartMin, true).show();
            }
        });

        // 終了時間ボタン
        mBtnQuietEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mQuietEndHour = hourOfDay;
                        mQuietEndMin = minute;
                        saveQuietHours();
                        updateQuietButtons();
                    }
                }, mQuietEndHour, mQuietEndMin, true).show();
            }
        });

        // シナリオ登録
        registerScenario();

        // アラーム設定
        scheduleAlarm(this);

        // 状態表示
        long lastRun = prefs.getLong("last_run", 0);
        if (lastRun == 0) {
            mStatusTextView.setText("アラーム設定完了（1時間ごと）\nまだニュースを取得していません");
        } else {
            String lastTime = android.text.format.DateFormat.format("yyyy/MM/dd HH:mm", lastRun).toString();
            int readCount = prefs.getInt("last_read_count", 0);
            mStatusTextView.setText("アラーム設定完了（1時間ごと）\n前回: " + lastTime + " (" + readCount + "件読み上げ)");
        }

        // 初回は即座にニュース読み上げを実行（静寂時間外のみ）
        if (!NewsAlarmReceiver.isQuietHours(this)) {
            Intent newsIntent = new Intent(this, NewsReadActivity.class);
            startActivity(newsIntent);
        } else {
            mStatusTextView.setText(mStatusTextView.getText() + "\n（現在は静寂時間帯です）");
        }
    }

    private void updateQuietButtons() {
        mBtnQuietStart.setText(String.format("%02d:%02d", mQuietStartHour, mQuietStartMin));
        mBtnQuietEnd.setText(String.format("%02d:%02d", mQuietEndHour, mQuietEndMin));
    }

    private void saveQuietHours() {
        getSharedPreferences("nhknews", MODE_PRIVATE).edit()
                .putInt("quiet_start_hour", mQuietStartHour)
                .putInt("quiet_start_min", mQuietStartMin)
                .putInt("quiet_end_hour", mQuietEndHour)
                .putInt("quiet_end_min", mQuietEndMin)
                .apply();
        Log.d(TAG, "Quiet hours saved: " +
                String.format("%02d:%02d", mQuietStartHour, mQuietStartMin) + " - " +
                String.format("%02d:%02d", mQuietEndHour, mQuietEndMin));
    }

    /**
     * シナリオを登録する.
     */
    private void registerScenario() {
        Log.d(TAG, "registerScenario()");
        Intent intent = new Intent();
        RegisterScenarioService.start(this, intent, RegisterScenarioService.CMD_REQUEST_SCENARIO);
    }

    /**
     * AlarmManagerで1時間ごとの繰り返しアラームを設定する.
     */
    public static void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NewsAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // 既存のアラームをキャンセルしてから再設定
        alarmManager.cancel(pendingIntent);

        // 1時間後から1時間ごとに繰り返し
        alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS,
                ALARM_INTERVAL_MS,
                pendingIntent
        );

        Log.d(TAG, "Alarm scheduled: every " + (ALARM_INTERVAL_MS / 1000 / 60) + " minutes");
    }
}
