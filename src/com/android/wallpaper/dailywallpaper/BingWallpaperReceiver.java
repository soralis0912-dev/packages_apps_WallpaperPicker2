package com.android.wallpaper.dailywallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BingWallpaperReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BingWallpaper bingWallpaper = new BingWallpaper(context);
        bingWallpaper.setDailyBingWallpaper();
    }
}
