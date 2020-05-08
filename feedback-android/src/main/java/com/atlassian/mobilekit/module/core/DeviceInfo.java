package com.atlassian.mobilekit.module.core;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;


import java.util.Locale;
import java.util.UUID;

public final class DeviceInfo {

    private static final String NAMESPACE = "com.atlassian.mobilekit.module.core";
    private static final String STORE_NAME = NAMESPACE + ".preferences";
    private static final String UUID_KEY = NAMESPACE + ".UUID";
    private static final String ANDROID_OS = "Android OS";

    private final Context ctx;
    private final SharedPreferences store;

    // These use lazy initialization
    private String uuid;
    private String udid;
    private String appVersionName;
    private int appVersionCode = -1;

    public DeviceInfo(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        store = ctx.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);
    }

    private synchronized String initUdid() {
        String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            throw new AssertionError("ANDROID_ID setting was null");
        }
        return androidId;
    }

    private synchronized String initUuid() {
        String uuidStr = store.getString(UUID_KEY, null);
        if (uuidStr == null) {
            uuidStr = UUID.randomUUID().toString();
            SharedPreferences.Editor edit = store.edit();
            edit.putString(UUID_KEY, uuidStr);
            edit.apply();
        }
        return uuidStr;
    }

    public String getUuid() {
        if (uuid == null) {
            uuid = initUuid();
        }

        return uuid;
    }

    public String getUdid() {
        if (udid == null) {
            udid = initUdid();
        }

        return udid;
    }

    public String getAppPkgName() {
        return ctx.getPackageName();
    }

    public String getAppName() {
        return ctx.getPackageManager()
                .getApplicationLabel(ctx.getApplicationInfo())
                .toString();
    }

    public String getAppVersionName() {
        if (appVersionName == null) {
            PackageInfo pInfo = getPackageInfo();
            appVersionName = pInfo.versionName;
        }

        return appVersionName;
    }

    public int getAppVersionCode() {
        if (appVersionCode == -1) {
            PackageInfo pInfo = getPackageInfo();
            appVersionCode = pInfo.versionCode;
        }

        return appVersionCode;
    }

    public String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    public String getSystemName() {
        return ANDROID_OS;
    }

    public String getDeviceName() {
        return Build.DEVICE;
    }

    public String getModel() {
        return Build.MODEL;
    }

    public String getLanguage() {
        return Locale.getDefault().getDisplayLanguage();
    }

    public String getLocale() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    public boolean hasConnectivity() {
        ConnectivityManager cMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cMgr == null) {
            return false;
        }

        NetworkInfo nwInfo = cMgr.getActiveNetworkInfo();
        return nwInfo != null && nwInfo.isConnected();
    }


    private PackageInfo getPackageInfo() {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
