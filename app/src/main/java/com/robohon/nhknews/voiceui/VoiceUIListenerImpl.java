package com.robohon.nhknews.voiceui;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import jp.co.sharp.android.voiceui.VoiceUIListener;
import jp.co.sharp.android.voiceui.VoiceUIVariable;

/**
 * 音声UIからの通知時の共通処理を実装するクラス.
 */
public class VoiceUIListenerImpl implements VoiceUIListener {
    private static final String TAG = VoiceUIListenerImpl.class.getSimpleName();

    private ScenarioCallback mCallback;

    public static final int ACTION_START        = 0;
    public static final int ACTION_END          = 1;
    public static final int RESOLVE_VARIABLE    = 2;
    public static final int ACTION_CANCELLED    = 3;
    public static final int ACTION_REJECTED     = 4;

    public VoiceUIListenerImpl(Context context) {
        super();
        try {
            mCallback = (ScenarioCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + TAG);
        }
    }

    @Override
    public void onVoiceUIEvent(List<VoiceUIVariable> variables) {
        Log.v(TAG, "onVoiceUIEvent");
        if (VoiceUIVariableUtil.isTarget(variables, ScenarioDefinitions.TARGET)) {
            mCallback.onScenarioEvent(ACTION_START, variables);
        }
    }

    @Override
    public void onVoiceUIActionEnd(List<VoiceUIVariable> variables) {
        Log.v(TAG, "onVoiceUIActionEnd");
        if (VoiceUIVariableUtil.isTarget(variables, ScenarioDefinitions.TARGET)) {
            mCallback.onScenarioEvent(ACTION_END, variables);
        }
    }

    @Override
    public void onVoiceUIResolveVariable(List<VoiceUIVariable> variables) {
        Log.v(TAG, "onVoiceUIResolveVariable");
        mCallback.onScenarioEvent(RESOLVE_VARIABLE, variables);
    }

    @Override
    public void onVoiceUIActionCancelled(List<VoiceUIVariable> variables) {
        Log.v(TAG, "onVoiceUIActionCancelled");
        if (VoiceUIVariableUtil.isTarget(variables, ScenarioDefinitions.TARGET)) {
            mCallback.onScenarioEvent(ACTION_CANCELLED, variables);
        }
    }

    @Override
    public void onVoiceUIRejection(VoiceUIVariable variable) {
        Log.v(TAG, "onVoiceUIRejection : " + variable.getStringValue());
        List<VoiceUIVariable> variables = new ArrayList<VoiceUIVariable>();
        variables.add(variable);
        mCallback.onScenarioEvent(ACTION_REJECTED, variables);
    }

    @Override
    public void onVoiceUISchedule(int i) {
        //処理不要
    }

    public static interface ScenarioCallback {
        public void onScenarioEvent(int event, List<VoiceUIVariable> variables);
    }
}
