package com.tracking.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.tracking.preview.CameraController;
import com.tracking.preview.CameraHelper;
import com.tracking.preview.CameraRecordRenderer;
import com.tracking.preview.CommonHandlerListener;

import org.opencv.android.CameraGLSurfaceView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;

import static org.opencv.imgproc.Imgproc.COLOR_YUV2RGB_I420;

public class CameraSurfaceView extends AutoFitGLSurfaceView
        implements CommonHandlerListener, SurfaceTexture.OnFrameAvailableListener {
    private final String TAG = "CameraSurfaceView";
    private final static int STATE_DEFAULT = 0;
    private final static int STATE_INIT = 11;
    private final static int STATE_UPDATE = 12;

    private CameraHandler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private CameraRecordRenderer mCameraRenderer;
    private int mState = STATE_DEFAULT;
    private Tracker mTracker;
    private Rect2d referenceRect = new Rect2d();
    private boolean enableTracking = false;
    private boolean enableCapture = true;
    private int previewWidth;
    private int previewHeight;

    private long mBeginTime = 0;
    private long mEndTime = 0;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        setEGLContextClientVersion(2);

        mHandlerThread = new HandlerThread("CameraHandlerThread");
        mHandlerThread.start();

        mBackgroundHandler = new CameraHandler(mHandlerThread.getLooper(), this);
        mCameraRenderer =
                new CameraRecordRenderer(context.getApplicationContext(), mBackgroundHandler);

        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public CameraRecordRenderer getRenderer() {
        return mCameraRenderer;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        CameraController.getInstance().release();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                // 跨进程 清空 Renderer数据
                mCameraRenderer.notifyPausing();
            }
        });
        super.onPause();
    }

    public void onDestroy() {
        enableTracking = false;
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mCameraRenderer.onDestroy();
        CameraController.getInstance().release();
        if (!mHandlerThread.isInterrupted()) {
            try {
                mHandlerThread.quit();
                mHandlerThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void changeNoneFilter() {
        mCameraRenderer.changeNoneFilter();
    }

    public void changeInnerFilter() {
        mCameraRenderer.changeInnerFilter();
    }

    public void changeExtensionFilter() {
        mCameraRenderer.changeExtensionFilter();
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public static class CameraHandler extends Handler {
        public static final int SETUP_CAMERA = 1001;
        public static final int CONFIGURE_CAMERA = 1002;
        public static final int START_CAMERA_PREVIEW = 1003;
        //public static final int STOP_CAMERA_PREVIEW = 1004;
        private CommonHandlerListener listener;

        public CameraHandler(Looper looper, CommonHandlerListener listener) {
            super(looper);
            this.listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            listener.handleMessage(msg);
        }
    }

    private int mPreviewFormat =  ImageFormat.NV21;
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case CameraHandler.SETUP_CAMERA: {
                final int width = msg.arg1;
                final int height = msg.arg2;
                final SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                surfaceTexture.setOnFrameAvailableListener(this);

                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraController.getInstance()
                                .setupCamera(surfaceTexture, getContext().getApplicationContext(),
                                        width);
                        mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(
                                CameraSurfaceView.CameraHandler.CONFIGURE_CAMERA, width, height));
                    }
                });
            }
            break;
            case CameraHandler.CONFIGURE_CAMERA: {
                final int width = msg.arg1;
                final int height = msg.arg2;
                Camera.Size previewSize = CameraHelper.getOptimalPreviewSize(
                        CameraController.getInstance().getCameraParameters(),
                        CameraController.getInstance().mCameraPictureSize, width);

                CameraController.getInstance().configureCameraParameters(previewSize);
                mPreviewFormat =  CameraController.getInstance().getPreviewFormat();
                if (previewSize != null) {
                    previewWidth = previewSize.width;
                    previewHeight = previewSize.height;
                    mCameraRenderer.setCameraPreviewSize(previewSize.height, previewSize.width);
                }
                mBackgroundHandler.sendEmptyMessage(CameraHandler.START_CAMERA_PREVIEW);
            }
            break;

            case CameraHandler.START_CAMERA_PREVIEW:
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraController.getInstance().startCameraPreview();
                        CameraController.getInstance().setCameraCallBack(new CameraController.CameraPreViewDataCallback() {
                            @Override
                            public void onGetFrame(byte[] data) {
                                //Camera.Size size = CameraController.getInstance().mCameraPictureSize;
                                //Log.i(TAG, "jerrypxiao data width =" + size.width + ", data height = "+ size.height);
                                if(enableTracking && enableCapture && mTracker != null){
                                    enableCapture = false;
                                    Log.i(TAG, "jerrypxiao onGetFrame  referencerect = (" + referenceRect.tl().x+ ", " + referenceRect.tl().y + ", " + referenceRect.br().x+ ", " + referenceRect.br().y);
                                    mBeginTime = SystemClock.elapsedRealtimeNanos();
                                    //trackingAsync(data, previewWidth, previewHeight, referenceRect);

                                    Mat frameMat = new Mat(previewHeight, previewWidth, CvType.CV_8UC1);
                                    frameMat.put(0,0, data);
                                    Mat rgbMat = new Mat();
                                    int code = mPreviewFormat == ImageFormat.NV21 ? Imgproc.COLOR_YUV2RGB_NV21 : COLOR_YUV2RGB_I420;
                                    Imgproc.cvtColor(frameMat, rgbMat, Imgproc.COLOR_YUV2RGB_I420, 4);

                                    Mat dst=rgbMat.clone();
                                    //复制矩阵进入dst

                                    Point center =new Point(rgbMat.width()/2.0,rgbMat.height()/2.0);
                                    Mat affineTrans=Imgproc.getRotationMatrix2D(center, 270.0, 1.0);

                                    Imgproc.warpAffine(rgbMat, dst, affineTrans, dst.size(),Imgproc.INTER_NEAREST);

                                    //Log.i(TAG, "jerrypxiao onGetFrame  rgbMat.rows() =  " + rgbMat.rows() + ", rgbMat.cols() =" + rgbMat.cols());
                                    final Bitmap mCacheBitmap = createBitmapfromMat(rgbMat);

                                    new Handler(Looper.getMainLooper()).post(new Runnable() { // Tried new Handler(Looper.myLopper()) also
                                        @Override
                                        public void run() {
                                            if(mDetectCallback != null){
                                                mDetectCallback.onDetectedBitmap(mCacheBitmap);
                                            }
                                        }
                                    });

                                }
                            }
                        });//add

                    }
                });

                break;

            default:
                break;
        }
    }

    public Bitmap createBitmapfromMat(Mat snap){
        Bitmap bp = Bitmap.createBitmap(snap.cols(), snap.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(snap, bp);
        return bp;
    }

    private synchronized void trackingAsync(final byte[] data, final int width, final int height, final Rect2d rect2d){
        new AsyncTask<Void, Void, Rect2d>(){
            @Override
            protected Rect2d doInBackground(Void... params) {
                Mat frameMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
                frameMat.put(0,0, data);
                //Imgproc.cvtColor(frameMat, cvt3Mat, Imgproc.COLOR_RGBA2RGB, 3);
                Mat rgbMat = new Mat();
                int code = mPreviewFormat == ImageFormat.NV21 ? Imgproc.COLOR_YUV2RGB_NV21 : COLOR_YUV2RGB_I420;
                Imgproc.cvtColor(frameMat, rgbMat, code, 3);
                Mat cvt3Mat = new Mat(rgbMat.size(), CvType.CV_8UC3);
                Imgproc.cvtColor(rgbMat, cvt3Mat, Imgproc.COLOR_RGBA2RGB, 3);

                if(mState == STATE_INIT){
                    Log.i(TAG, "jerrypxiao trackingAsync  rect2d = (" + rect2d.tl().x+ ", " + rect2d.tl().y + ", "
                            + rect2d.br().x+ ", " + rect2d.br().y);
                    if (mTracker != null) {
                        mTracker.init(cvt3Mat, rect2d);
                    }
                    return null;
                }else if(mState == STATE_UPDATE){
                    Rect2d rect2d = new Rect2d();
                    if (mTracker != null) {
                        mTracker.update(cvt3Mat, rect2d);
                    }
                    Log.e(TAG, "jerrypxiao STATE_UPDATE = (" + rect2d.tl().x+ ", " + rect2d.tl().y + ", " + rect2d.br().x+ ", " + rect2d.br().y);
                    return rect2d;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Rect2d rect2d) {
                if(mState == STATE_INIT){
                    mState = STATE_UPDATE;
                }else if(mState == STATE_UPDATE){
                  if(mDetectCallback != null){
                      Log.i(TAG, "jerrypxiao onPostExecute rect2d =" + rect2d.tl() + ", rect2d.br() = "+ rect2d.br() );
                      mEndTime = SystemClock.elapsedRealtimeNanos();
                      Log.i(TAG, "jerrypxiao cos time =" + (mEndTime - mBeginTime)/1000000 );
                      mDetectCallback.onRectDetected(rect2d);
                  }
                }
                enableCapture = true;
            }
        }.execute();
    }

    public void initTracker(String tracker){
        mTracker = Tracker.create(tracker);
    }

    public void startTracking(RectF rectF){
        if(rectF == null){
            return;
        }
        mState = STATE_INIT;
        Log.i(TAG, "jerrypxiao startTracking rectF =" + rectF.top + ","+ rectF.left
                + ","+ rectF.bottom + "," +rectF.right);
        referenceRect = new Rect2d(rectF.left, rectF.top, rectF.width(), rectF.height());
        enableTracking = true;
    }

    public void setDetectCallback(DetectCallback callback){
        mDetectCallback = callback;
    }

    public void reset( ) {
        mState = STATE_INIT;
        referenceRect = new Rect2d();
        enableTracking = true;
        mTracker = null;
    }

    private DetectCallback mDetectCallback;
    public interface DetectCallback{
        void onRectDetected(Rect2d rect2d);
        void onDetectedBitmap(Bitmap bitmap);
    }
}