package com.tracking.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.tracking.utils.Utils;

import java.io.IOException;

/**
 * Created by jerrypxiao on 2017/7/17.
 */

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private final static String TAG = "CameraTextureView";
    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;
    private Camera mCamera;
    public int mCameraWidth = 0;
    public int mCameraHeight = 0;
    private byte[] mPreviewFrame1;
    private byte[] mPreviewFrame2;
    public boolean mSupportUserBuffer = true;
    private CameraDataListener mListener;

    long mDrawTimeStamp = 0;
    int mDrawFrame = 0;
    float mDrawFps = 0;


    public CameraTextureView(Context context) {
        super(context);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = android.hardware.Camera.open();

        Camera.Parameters parameters = mCamera.getParameters();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= 400 && size.height <= 400 && size.height >= mCameraHeight && size.width >= mCameraWidth) {
                mCameraHeight = size.height;
                mCameraWidth = size.width;
            }
        }
        //Camera 默认是横屏，高和宽度相反
        int textureWidth = Utils.getDeviceWidth();
        int textureHeight = Utils.getDeviceWidth() * mCameraWidth / mCameraHeight;
        //setAspectRatio(textureWidth, textureHeight);
        android.util.Log.e("lable", "mCameraWidth ="+ mCameraWidth + ",mCameraHeight = " + mCameraHeight);
        setLayoutParams(new FrameLayout.LayoutParams(
                textureWidth, textureHeight));
        if(mListener != null){
            mListener.resetLayout(textureWidth, textureHeight, mCameraWidth, mCameraHeight);
        }

        parameters.setPreviewSize(mCameraWidth, mCameraHeight);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        int bitpixel = ImageFormat.getBitsPerPixel(parameters.getPreviewFormat());
        float byteNum = bitpixel * 1.0f/Byte.SIZE;
        int bufSize = (int)(mCameraHeight * mCameraWidth * byteNum);

        try {
            mCamera.setPreviewTexture(surface);
        } catch (IOException t) {
        }

        if(mSupportUserBuffer){
            mPreviewFrame1 = new byte[bufSize];
            mPreviewFrame2 = new byte[bufSize];
            mCamera.addCallbackBuffer(mPreviewFrame1);
            mCamera.addCallbackBuffer(mPreviewFrame2);
            mCamera.setPreviewCallbackWithBuffer(this);
        }else{
            mCamera.setPreviewCallback(this);
        }
        mCamera.startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Update your view here!
        if(mDrawTimeStamp == 0){
            mDrawTimeStamp = System.currentTimeMillis();
        }
        mDrawFrame++;
        if (mDrawFrame >= 30) {
            mDrawFrame = 0;
            long currentTimeStamp = System.currentTimeMillis();
            mDrawFps =  (30 * 1000.0f / (currentTimeStamp - mDrawTimeStamp));
            mDrawTimeStamp = currentTimeStamp;
            updateFps();

        }
    }

    Handler mHandler = new Handler(Looper.getMainLooper());
    protected void updateFps(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                /*MGTracker.DebugInfo debugInfo = MGTracker.newInstance().getDebugInfo();
                if(debugInfo != null && mIsInitTrack){
                    String logInfo = String.format("绘制帧率:%.2f 追踪帧率:%.2f 跟踪耗时%.2f \n缩放：%.2f 密度: %d 结果：%.1f 预测结果：%d\n特征点:活动:%d|目标:%d|背景:%d|预测:%d|RDTD:%d",
                            mDrawFps,debugInfo.trackFrame,debugInfo.trackCost,debugInfo.trackScale,debugInfo.trackDensity,debugInfo.matchPercent,debugInfo.isMatch,
                            debugInfo.activePoints,debugInfo.targetPoints,debugInfo.framePoints,debugInfo.predictPoints,debugInfo.rdtdCount);
                    mInfoView.setText(logInfo);
                }else{
                    mInfoView.setText("绘制fps:"+mDrawFps);
                }*/
                if(mListener != null){
                    mListener.onGetFPS(mDrawFps);
                }

            }
        });
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try{
            if(mListener != null){
                mListener.onGetFrame(data);
            }
        }finally {
            if(mSupportUserBuffer){
                mCamera.addCallbackBuffer(data);
            }
        }
    }

    public void setCameraDataListener(CameraDataListener listener){
        mListener = listener;
        setSurfaceTextureListener(this);
    }

    public interface CameraDataListener {
        void onGetFrame(byte[] data);
        void resetLayout(int width, int height, int cameraWidth, int cameraHeight);
        void onGetFPS(float fps);
    }
}
