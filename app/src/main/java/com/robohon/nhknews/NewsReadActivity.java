package com.robohon.nhknews;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.co.sharp.android.voiceui.VoiceUIManager;
import jp.co.sharp.android.voiceui.VoiceUIVariable;

import com.robohon.nhknews.voiceui.RegisterScenarioService;
import com.robohon.nhknews.voiceui.ScenarioDefinitions;
import com.robohon.nhknews.voiceui.VoiceUIListenerImpl;
import com.robohon.nhknews.voiceui.VoiceUIManagerUtil;
import com.robohon.nhknews.voiceui.VoiceUIVariableUtil;

/**
 * ニュース読み上げActivity.
 * AlarmManagerから起動され、RSSフィードからニュースを取得し、
 * 未読のタイトルをロボホンに読み上げさせる.
 */
public class NewsReadActivity extends Activity implements VoiceUIListenerImpl.ScenarioCallback {
    public static final String TAG = NewsReadActivity.class.getSimpleName();

    private VoiceUIManager mVUIManager = null;
    private VoiceUIListenerImpl mVUIListener = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private TextView mStatusTextView;

    /** 読み上げるニュースタイトルのキュー */
    private List<String> mNewsTitles = new ArrayList<>();
    private int mCurrentIndex = 0;
    private boolean mSpeechInProgress = false;
    private Runnable mFallbackRunnable = null;

    /** ホームボタン検知 */
    private HomeEventReceiver mHomeEventReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        System.setProperty("http.keepAlive", "false");

        setContentView(R.layout.activity_news_read);
        mStatusTextView = (TextView) findViewById(R.id.news_status_text);
        mStatusTextView.setText("ニュースを取得中...");

        // ホームボタン検知
        mHomeEventReceiver = new HomeEventReceiver();
        IntentFilter filterHome = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeEventReceiver, filterHome);

        // シナリオ登録
        Intent scenarioIntent = new Intent();
        RegisterScenarioService.start(this, scenarioIntent, RegisterScenarioService.CMD_REQUEST_SCENARIO);

        // バックグラウンドでニュース取得
        new Thread(new Runnable() {
            @Override
            public void run() {
                fetchAndReadNews();
            }
        }).start();
    }

    /**
     * ニュースを取得し、未読のものを読み上げキューに追加する.
     */
    private void fetchAndReadNews() {
        // RSS取得
        List<NewsFetcher.NewsItem> allItems = NewsFetcher.fetchNews();

        if (allItems.isEmpty()) {
            updateStatus("ニュースを取得できませんでした");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
            return;
        }

        // 1時間以内のニュースをフィルタ
        List<NewsFetcher.NewsItem> recentItems = NewsFetcher.filterRecentNews(allItems);

        // 既読フィルタ
        SharedPreferences prefs = getSharedPreferences("nhknews", MODE_PRIVATE);
        Set<String> readTitles = prefs.getStringSet("read_titles", new HashSet<String>());

        List<String> unreadTitles = new ArrayList<>();
        Set<String> newReadTitles = new HashSet<>(readTitles);

        for (NewsFetcher.NewsItem item : recentItems) {
            if (!readTitles.contains(item.title)) {
                unreadTitles.add(item.title);
                newReadTitles.add(item.title);
            }
        }

        // 既読リストを更新（最新100件のみ保持）
        if (newReadTitles.size() > 100) {
            Set<String> trimmed = new HashSet<>();
            for (NewsFetcher.NewsItem item : allItems) {
                trimmed.add(item.title);
            }
            newReadTitles = trimmed;
        }

        prefs.edit()
                .putStringSet("read_titles", newReadTitles)
                .putLong("last_run", System.currentTimeMillis())
                .putInt("last_read_count", unreadTitles.size())
                .apply();

        if (unreadTitles.isEmpty()) {
            updateStatus("新しいニュースはありません（" + recentItems.size() + "件中0件が未読）");
            Log.d(TAG, "No unread news");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
            return;
        }

        // 読み上げキューをセット（語尾に「だって。」を付ける）
        mNewsTitles = new ArrayList<>();
        for (String title : unreadTitles) {
            mNewsTitles.add(title + "、だって。");
        }
        mCurrentIndex = 0;

        Log.d(TAG, "Unread news: " + unreadTitles.size() + " items");
        updateStatus("未読ニュース: " + unreadTitles.size() + "件");

        // VoiceUI初期化して読み上げ開始（シナリオ登録完了を待つ）
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initVoiceUIAndStart();
            }
        }, 2500);
    }

    /**
     * VoiceUIを初期化してニュース読み上げを開始する.
     */
    private void initVoiceUIAndStart() {
        if (mVUIManager == null) {
            mVUIManager = VoiceUIManager.getService(this);
        }
        if (mVUIListener == null) {
            mVUIListener = new VoiceUIListenerImpl(this);
        }
        VoiceUIManagerUtil.registerVoiceUIListener(mVUIManager, mVUIListener);
        VoiceUIManagerUtil.enableScene(mVUIManager, ScenarioDefinitions.SCENE_COMMON);

        // 「新しいNHKニュースだよ」の後、ニュースを順番に読み上げ
        speakText("新しいNHKニュースだよ。");
    }

    /**
     * 次のニュースを読み上げる.
     */
    private void speakNext() {
        if (mCurrentIndex < mNewsTitles.size()) {
            String text = mNewsTitles.get(mCurrentIndex);
            mCurrentIndex++;
            speakText(text);
        } else {
            // 全て読み上げ完了 → アプリを終了
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 1000);
        }
    }

    /**
     * テキストを発話する.
     * HVMLコールバックまたはタイマーで次に進む.
     */
    private void speakText(final String text) {
        // 前のフォールバックタイマーをキャンセル
        if (mFallbackRunnable != null) {
            mHandler.removeCallbacks(mFallbackRunnable);
            mFallbackRunnable = null;
        }

        mSpeechInProgress = true;
        updateStatus(text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VoiceUIManagerUtil.setMemory(mVUIManager, "news_text", text);
                int result = VoiceUIManagerUtil.startSpeech(mVUIManager, ScenarioDefinitions.ACC_NEWS_READ);
                Log.d(TAG, "startSpeech result=" + result + " text=" + text);

                // テキスト長さに応じた待ち時間（1文字約250ms + 余裕5秒）
                long waitMs = Math.max(8000, text.length() * 250 + 5000);
                mFallbackRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (mSpeechInProgress) {
                            Log.d(TAG, "Timer fallback: moving to next");
                            mSpeechInProgress = false;
                            speakNext();
                        }
                    }
                };
                mHandler.postDelayed(mFallbackRunnable, waitMs);
            }
        });
    }

    /**
     * VoiceUIからのコールバック.
     */
    @Override
    public void onScenarioEvent(int event, List<VoiceUIVariable> variables) {
        Log.v(TAG, "onScenarioEvent() event=" + event);
        switch (event) {
            case VoiceUIListenerImpl.ACTION_START:
                if (VoiceUIVariableUtil.isTargetFuncution(variables, ScenarioDefinitions.TARGET, "speech_done")) {
                    Log.d(TAG, "speech_done callback received");
                    if (mSpeechInProgress) {
                        mSpeechInProgress = false;
                        // フォールバックタイマーをキャンセル
                        if (mFallbackRunnable != null) {
                            mHandler.removeCallbacks(mFallbackRunnable);
                            mFallbackRunnable = null;
                        }
                        speakNext();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void updateStatus(final String text) {
        Log.d(TAG, "Status: " + text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mStatusTextView != null) {
                    mStatusTextView.setText(text);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVUIManager == null) {
            mVUIManager = VoiceUIManager.getService(this);
        }
        if (mVUIListener == null) {
            mVUIListener = new VoiceUIListenerImpl(this);
        }
        VoiceUIManagerUtil.registerVoiceUIListener(mVUIManager, mVUIListener);
        VoiceUIManagerUtil.enableScene(mVUIManager, ScenarioDefinitions.SCENE_COMMON);
    }

    @Override
    public void onPause() {
        super.onPause();
        VoiceUIManagerUtil.stopSpeech();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHomeEventReceiver);
        mVUIManager = null;
        mVUIListener = null;
    }

    private class HomeEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Home button pressed");
            finish();
        }
    }
}
