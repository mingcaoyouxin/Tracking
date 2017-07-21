package com.tracking.utils;

import android.content.res.Resources;

/**
 * Created by jerrypxiao on 2017/7/17.
 */

public class Utils {
    public static int getDeviceWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static int getDeviceHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
