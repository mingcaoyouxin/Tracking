package com.tracking.sdk;


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

    public native void openTrack(byte[] yuvData,int dataType, long x,
                                 long y, long w, long h,
                                 int cameraWidth, int cameraHeight);

    public native void processTrack(byte[] yuvData,int dataType,
                                    int cameraWidth, int cameraHeight);

    public static native float[] CMTgetRect(int cameraWidth, int cameraHeight);

    public native boolean CMTisTrackValid();

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
