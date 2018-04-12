package vip.wangzs.imagequads.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Created by wangzs on 2018/4/12.
 */

public class SpConfigUtil {
    public final static String SHAPE_MODE = "shape_mode";
    public final static String QUAD_BG_COLOR = "quad_bg_color";

    private final static String SP_CONFIG_NAME = "quad_config";

    public static void put(Context context, String key, Object object) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof String) {
            editor.putString(key, (String) object);
        } else {
            editor.putString(key, object.toString());
        }
        editor.apply();
    }

    public static Object get(Context context, String key, Object defaultObject) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        }
        return null;
    }

    public static void delete(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.apply();
    }

    public static void clearAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }

    public static boolean contains(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    public static Map<String, ?> getAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE);
        return sp.getAll();
    }
}
