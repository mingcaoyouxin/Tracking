package com.tracking.preview;


import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.tracking.Tracker;


public class TrackerManager {

    /**
     * 传入数据格式
     * NV21 Android摄像头默认格式
     */
    public static final int TYPE_NV21 = 0;
    public static final int TYPE_RGB = 1;
    public static final int TYPE_RGBA = 2;

    Tracker mTracker;

    private static class ObjTrackHolder{
        public static TrackerManager instance = new TrackerManager();
    }

    private TrackerManager(){
        init();
    }

    public static TrackerManager newInstance(){
        return ObjTrackHolder.instance;
    }

    public void init() {
        mTracker = Tracker.create("KCF");
    }


    public static class DebugInfo{
        public float trackFrame;
        public float trackCost;
        public float trackScale;
        public long trackDensity;
        public float matchPercent;
        public int isMatch;
        public int activePoints;
        public int targetPoints;
        public int framePoints;
        public int predictPoints;
        public int rdtdCount;
    }

}