package co.anybooks.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.view.Window;
import android.view.WindowManager;

public class BrightnessUtils {


    /**
     * 判断是否是自动模式
     */
    public static boolean isAutoBrightness(Context context) {
        try {
            int autoBrightness = Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (autoBrightness == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                return true;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getSystemBrightness(Context context) {
        int systemBrightness = 0;
        try {

            ContentResolver resolver = context.getContentResolver();

            if(isAutoBrightness(context)){

                systemBrightness  = (int) System.getFloat(resolver,"screen_auto_brightness_adj");


            }else {
                systemBrightness = Settings.System
                    .getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    public static void changeAppBrightness(Activity activity, int brightness) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(lp);
    }

}
