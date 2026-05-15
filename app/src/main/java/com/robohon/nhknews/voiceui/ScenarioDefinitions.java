package com.robohon.nhknews.voiceui;

/**
 * シナリオファイルで使用する定数の定義クラス.
 */
public class ScenarioDefinitions {

    private ScenarioDefinitions() {
    }

    /****************** 共通の定義 *******************/
    public static final String TAG_SCENE = "scene";
    public static final String TAG_ACCOST = "accost";
    public static final String TAG_MEMORY_P = "memory_p:";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_FUNCTION = "function";

    /****************** アプリ固有の定義 *******************/
    protected static final String PACKAGE = "com.robohon.nhknews";
    public static final String TARGET = PACKAGE;
    public static final String SCENE_COMMON = PACKAGE + ".scene_common";

    /** accost名：ニュース読み上げ */
    public static final String ACC_NEWS_READ = PACKAGE + ".news.read";
}
