package com.tracking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    // 公共模块
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar    DETECT_RECT_COLOR     = new Scalar(0, 255, 255, 0);
    private Button btnProc;
    private ImageView imageView;
    private Bitmap bmp;
    private Tracker mTracker;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("OpenCV");
        System.loadLibrary("opencv_java3");
    }

    public static native int[] gray(int[] buf, int w, int h);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("MainActivity", "OpenCV loaded successfully");
                    mTracker = Tracker.create("KCF");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void initView() {
        btnProc = (Button) findViewById(R.id.btn_gray_process);
        imageView = (ImageView) findViewById(R.id.image_view);
        //将lena图像加载程序中并进行显示
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic);
        imageView.setImageBitmap(bmp);

        btnProc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat rgbMat = new Mat();
                Mat grayMat = new Mat();
                //获取lena彩色图像所对应的像素数据
                Utils.bitmapToMat(bmp, rgbMat);
                /*
                //将彩色图像数据转换为灰度图像数据并存储到grayMat中
                Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
                //创建一个灰度图像
                Bitmap grayBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.RGB_565);
                //将矩阵grayMat转换为灰度图像
                Point startPoint = new Point(900, 100);
                Point endPoint  = new Point(1200, 400);
                Imgproc.rectangle(grayMat, startPoint, endPoint, DETECT_RECT_COLOR, 3);

                Utils.matToBitmap(grayMat, grayBmp);
                imageView.setImageBitmap(grayBmp);
                */
                //-------------------test---------------

                Mat mSrcMat = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
                int w = bmp.getWidth();
                int h = bmp.getHeight();
                int[] pixels = new int[w*h];
                bmp.getPixels(pixels, 0, w, 0, 0, w, h);
                int[] resultInt = gray(pixels, w, h);
                Bitmap resultImg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                resultImg.setPixels(resultInt, 0, w, 0, 0, w, h);
                imageView.setImageBitmap(resultImg);
/*
                Mat mHueImage = new Mat(rgbMat.size(), CvType.CV_8UC1);
                Imgproc.cvtColor(rgbMat, mHueImage, Imgproc.COLOR_RGBA2RGB, 3);

                Point startPoint = new Point(900, 100);
                Point endPoint  = new Point(1200, 400);

                Rect2d referenceRect = new Rect2d(startPoint.x, startPoint.y, 300, 300);
                mTracker.init(mHueImage, referenceRect);
                Imgproc.rectangle(mHueImage, startPoint, endPoint, FACE_RECT_COLOR, 3);

                Mat rotationMatrix2D = Imgproc.getRotationMatrix2D(new Point(mHueImage.rows() / 2, mHueImage.cols() / 2), -45, 1.0f);
                Mat finalMat = new Mat();
                Imgproc.warpAffine(mHueImage, finalMat, rotationMatrix2D, rgbMat.size());

                Rect2d rect2d = new Rect2d();
                mTracker.update(mHueImage, rect2d);
                Log.i(TAG, "jerrypxiao touch  referencerect = (" + referenceRect.tl().x+ ", " + referenceRect.tl().y + ", " + referenceRect.br().x+ ", " + referenceRect.br().y);
                Log.e(TAG, "jerrypxiao touch  referencerect = (" + rect2d.tl().x+ ", " + rect2d.tl().y + ", " + rect2d.br().x+ ", " + rect2d.br().y);
                Imgproc.rectangle(mHueImage, rect2d.tl(), rect2d.br(), DETECT_RECT_COLOR, 3);


                Bitmap grayBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.RGB_565);
                //将矩阵grayMat转换为灰度图像
                Utils.matToBitmap(finalMat, grayBmp);
                imageView.setImageBitmap(grayBmp);
*/
            }
        });
    }
}
