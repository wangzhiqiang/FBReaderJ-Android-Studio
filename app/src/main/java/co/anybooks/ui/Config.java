package co.anybooks.ui;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {


    SharedPreferences sp;

    private static Config config = new Config();

    private Config() {
    }

    public void init(Context context) {
        sp = context.getSharedPreferences("core-reader-config", Context.MODE_PRIVATE);

    }

    public static Config instance() {
        return config;
    }

    public void setConfig(String k, String v) {

        sp.edit().putString(k,v).apply();
    }

    public String getConfig(String k) {
        return sp.getString(k,null);
    }


}
