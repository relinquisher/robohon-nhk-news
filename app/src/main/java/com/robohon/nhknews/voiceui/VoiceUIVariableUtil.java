package com.robohon.nhknews.voiceui;

import java.util.Iterator;
import java.util.List;
import jp.co.sharp.android.voiceui.VoiceUIVariable;

/**
 * VoiceUIVariable関連のUtilityクラス.
 */
public final class VoiceUIVariableUtil {

    private VoiceUIVariableUtil(){}

    public static boolean isTarget(final List<VoiceUIVariable> variableList, final String target) {
        boolean result = false;
        for (int i = 0; i < variableList.size(); i++) {
            if (getVariableData(variableList, ScenarioDefinitions.ATTR_TARGET).equals(target)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static boolean isTargetFuncution(final List<VoiceUIVariable> variableList, final String target, final String function) {
        boolean result = false;
        for (int i = 0; i < variableList.size(); i++) {
            if (getVariableData(variableList, ScenarioDefinitions.ATTR_TARGET).equals(target)) {
                if(getVariableData(variableList, ScenarioDefinitions.ATTR_FUNCTION).equals(function)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static String getVariableData(final List<VoiceUIVariable> variableList, final String name) {
        String result = "";
        int index = getListIndex(variableList, name);
        if (index != -1) {
            result = variableList.get(index).getStringValue();
        }
        return result;
    }

    public static int getListIndex(final List<VoiceUIVariable> variableList, final String name) {
        int index = -1;
        int tmp = 0;
        Iterator<VoiceUIVariable> it = variableList.iterator();
        while (it.hasNext()) {
            VoiceUIVariable variable = it.next();
            if (variable.getName().equals(name)) {
                index = tmp;
                break;
            }
            tmp++;
        }
        return index;
    }
}
