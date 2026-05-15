package com.robohon.nhknews.voiceui;

import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import jp.co.sharp.android.voiceui.VoiceUIListener;
import jp.co.sharp.android.voiceui.VoiceUIManager;
import jp.co.sharp.android.voiceui.VoiceUIVariable;

import static com.robohon.nhknews.voiceui.ScenarioDefinitions.TAG_ACCOST;

/**
 * VoiceUIManager関連のUtilityクラス.
 */
public class VoiceUIManagerUtil {
    public static final String TAG = VoiceUIManagerUtil.class.getSimpleName();

    private VoiceUIManagerUtil(){}

    public static int registerVoiceUIListener(VoiceUIManager vm, VoiceUIListener listener) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        if (vm != null) {
            try {
                result = vm.registerVoiceUIListener(listener);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed registerVoiceUIListener.[" + e.getMessage() + "]");
            }
        }
        return result;
    }

    public static int unregisterVoiceUIListener(VoiceUIManager vm, VoiceUIListener listener) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        if (vm != null) {
            try {
                result = vm.unregisterVoiceUIListener(listener);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed unregisterVoiceUIListener.[" + e.getMessage() + "]");
            }
        }
        return result;
    }

    public static int enableScene(VoiceUIManager vm, final String scene) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        Log.d(TAG, "enableScene() scene=" + scene);
        if (vm == null || scene == null || "".equals(scene)) {
            return result;
        }
        VoiceUIVariable variable = new VoiceUIVariable(ScenarioDefinitions.TAG_SCENE, scene);
        variable.setExtraInfo(VoiceUIManager.SCENE_ENABLE);
        ArrayList<VoiceUIVariable> listVariables = new ArrayList<>();
        listVariables.add(variable);
        try {
            result = vm.updateAppInfo(listVariables);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed updateAppInfo.[" + e.getMessage() + "]");
        }
        return result;
    }

    public static int disableScene(VoiceUIManager vm, final String scene) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        if (vm == null || scene == null || "".equals(scene)) {
            return result;
        }
        VoiceUIVariable variable = new VoiceUIVariable(ScenarioDefinitions.TAG_SCENE, scene);
        variable.setExtraInfo(VoiceUIManager.SCENE_DISABLE);
        ArrayList<VoiceUIVariable> listVariables = new ArrayList<>();
        listVariables.add(variable);
        try {
            result = vm.updateAppInfo(listVariables);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed updateAppInfo.[" + e.getMessage() + "]");
        }
        return result;
    }

    public static int startSpeech(VoiceUIManager vm, final String accost) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        Log.d(TAG, "startSpeech() accost=" + accost);
        if (vm == null || accost == null || "".equals(accost)) {
            return result;
        }
        VoiceUIVariable variable = new VoiceUIVariable(TAG_ACCOST, accost);
        ArrayList<VoiceUIVariable> variables = new ArrayList<>();
        variables.add(variable);
        try {
            result = vm.updateAppInfoAndSpeech(variables);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed updateAppInfoAndSpeech.[" + e.getMessage() + "]");
        }
        return result;
    }

    public static void stopSpeech() {
        try {
            VoiceUIManager.stopSpeech();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed StopSpeech.[" + e.getMessage() + "]");
        }
    }

    public static int setMemory(VoiceUIManager vm, final String key, final String value) {
        int result = VoiceUIManager.VOICEUI_ERROR;
        String name;
        if (vm == null || key == null || "".equals(key)) {
            return result;
        } else {
            name = ScenarioDefinitions.TAG_MEMORY_P + key;
        }
        VoiceUIVariable variable = new VoiceUIVariable(name, value);
        ArrayList<VoiceUIVariable> variables = new ArrayList<>();
        variables.add(variable);
        try {
            result = vm.updateAppInfo(variables);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed updateAppInfo.[" + e.getMessage() + "]");
        }
        return result;
    }
}
