package com.robohon.nhknews.voiceui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jp.co.sharp.android.voiceui.VoiceUIManager;

/**
 * シナリオ登録するためのサービス.
 */
public class RegisterScenarioService extends Service {
    private static final String TAG = RegisterScenarioService.class.getSimpleName();

    public static final int CMD_REQUEST_SCENARIO = 10;
    private static final String NAME_KEY_COMMAND = "key_cmd";
    private static final String SCENARIO_FOLDER_DEFAULT = "hvml";
    private static final String SCENARIO_FOLDER_HOME = "home";
    private static final String SCENARIO_FOLDER_OTHER = "other";
    private static final String KEY_LOCALE = "locale";

    private VoiceUIManager mVUIManager;

    public RegisterScenarioService() {
    }

    public static void start(Context context, Intent baseIntent, int command) {
        baseIntent.putExtra(NAME_KEY_COMMAND, command);
        baseIntent.setClass(context, RegisterScenarioService.class);
        context.startService(baseIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mVUIManager == null) {
            mVUIManager = VoiceUIManager.getService(getApplicationContext());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int cmd = intent.getIntExtra(NAME_KEY_COMMAND, -1);
        if (cmd == -1) {
            Log.e(TAG, "onStartCommand:not app key_command");
            return Service.START_NOT_STICKY;
        }

        Log.d(TAG, "onStartCommand cmd:" + cmd);
        switch (cmd) {
            case CMD_REQUEST_SCENARIO:
                String locale = intent.getStringExtra(KEY_LOCALE);
                registerScenario(locale, true);
                registerScenario(locale, false);
                stopSelf();
                break;
            default:
                break;
        }
        return Service.START_NOT_STICKY;
    }

    private void registerScenario(String locale, Boolean home) {
        Log.d(TAG, "registerScenario-S: " + locale + " : " + home.toString());

        File localFolder = null;
        if (home) {
            localFolder = this.createLocalFolder(SCENARIO_FOLDER_HOME);
        } else {
            localFolder = this.createLocalFolder(SCENARIO_FOLDER_OTHER);
        }
        if (localFolder == null) {
            Log.e(TAG, "can not make local folder");
            return;
        }

        String assetsFolderName = this.getAssetsScenarioFolderName(locale, home);

        final AssetManager assetManager = getResources().getAssets();
        String[] fileList = null;
        try {
            fileList = assetManager.list(assetsFolderName);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final String hvmlPrefix = getPackageName().replace(".","_");

        for (String fileName : fileList) {
            if (fileName.endsWith(".hvml")
                && fileName.startsWith(hvmlPrefix)
                && isAvailableFileName(fileName) ) {
                Log.d(TAG, "hvml files = " + fileName);
                this.copyFileFromAssetsToLocal(assetsFolderName, localFolder.getPath(), fileName);
            }
        }

        File[] files = localFolder.listFiles();

        for (File file : files) {
            Log.d(TAG, "registerScenario file=" + file.getAbsolutePath());
            int result = VoiceUIManager.VOICEUI_ERROR;
            try {
                if (home) {
                    result = mVUIManager.registerHomeScenario(file.getAbsolutePath());
                    if (result == VoiceUIManager.VOICEUI_ERROR)
                        Log.e(TAG, "registerScenario:Error");
                } else {
                    result = mVUIManager.registerScenario(file.getAbsolutePath());
                    if (result == VoiceUIManager.VOICEUI_ERROR)
                        Log.e(TAG, "registerScenario:Error");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "registerScenario-E:" + home.toString());
    }

    private String getAssetsScenarioFolderName(String locale, Boolean home) {
        String result = "";
        if (locale == null || "".equals(locale)) {
            if (home) {
                result = SCENARIO_FOLDER_DEFAULT + "/" + SCENARIO_FOLDER_HOME;
            } else {
                result = SCENARIO_FOLDER_DEFAULT + "/" + SCENARIO_FOLDER_OTHER;
            }
        } else {
            result = SCENARIO_FOLDER_DEFAULT + "_" + locale;
            final AssetManager assetManager = getResources().getAssets();
            String[] fileList = null;
            try {
                fileList = assetManager.list(result);
            } catch (IOException e) {
                e.printStackTrace();
                result = SCENARIO_FOLDER_DEFAULT;
            }
            if (fileList == null || fileList.length == 0) {
                Log.w(TAG, "not exist locale folder");
                result = SCENARIO_FOLDER_DEFAULT;
            }
            if (home) {
                result = result + "/" + SCENARIO_FOLDER_HOME;
            } else {
                result = result + "/" + SCENARIO_FOLDER_OTHER;
            }
        }
        Log.d(TAG, "getAssetsScenarioFolderName() result : " + result);
        return result;
    }

    private File createLocalFolder(String childPath) {
        File folder = null;
        try {
            folder = new File(this.getApplicationContext().getFilesDir(), childPath);
            if (!folder.exists()) {
                folder.mkdirs();
            } else {
                try {
                    recursiveDeleteFile(folder);
                } catch(Exception e) {
                    Log.e(TAG, "recursiveDeleteFile exception : " + e.getMessage());
                }
                folder.mkdirs();
            }
            folder.setReadable(true, false);
            folder.setWritable(true, false);
            folder.setExecutable(true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folder;
    }

    private void copyFileFromAssetsToLocal(String assetsFolderName, String localFolderName, String fileName) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            File assetsFile = new File(assetsFolderName, fileName);
            inputStream = getResources().getAssets().open(assetsFile.getPath());

            File localFile = new File(localFolderName, fileName);
            fileOutputStream = new FileOutputStream(localFile.getPath());
            localFile.setReadable(true, false);
            localFile.setWritable(true, false);
            localFile.setExecutable(true, false);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) >= 0) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try { fileOutputStream.close(); } catch (Exception e) {}
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception e) {}
            }
        }
    }

    private static boolean isAvailableFileName(final String fileName) {
        if (null == fileName || "".equals(fileName)) {
            return false;
        }
        final String ASCII = "^[\\u0020-\\u007E]+$";
        if (!fileName.matches(ASCII)) {
            return false;
        }
        final String regularExpression = "^.*[(<|>|:|\\*|?|\"|/|\\\\|\\|)].*$";
        if (fileName.matches(regularExpression)) {
            return false;
        }
        return true;
    }

    private static void recursiveDeleteFile(final File file) throws Exception {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDeleteFile(child);
            }
        }
        file.delete();
    }
}
