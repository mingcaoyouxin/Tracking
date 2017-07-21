package com.tracking.preview;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.tracking.R;
import com.tracking.widget.CameraSurfaceView;
import com.tracking.widget.ConfirmationDialogFragment;
import com.tracking.widget.NormalView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect2d;

/**
 * Created by jerrypxiao on 2017/6/7.
 */

public class PreviewActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener{
    private final static String TAG = "PreviewActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private CameraSurfaceView mCameraSurfaceView;
    private NormalView detectView;
    private Button mStartTrack;
    private Button mResetButton;
    private Spinner mSpinner;
    private String []mItems = new String[]{"KCF", "TLD", "MIL", "BOOSTING", "GOTURN", "MEDIANFLOW"};
    private String mCurrentTracker = mItems[0];

    private ImageView imageView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("PreviewActivity", "OpenCV loaded successfully");
                    //mCameraSurfaceView.initTracker();
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_frame);
        mCameraSurfaceView = (CameraSurfaceView)findViewById(R.id.cameraview);
        mCameraSurfaceView.setAspectRatio(3,4);
        detectView = (NormalView)findViewById(R.id.drawing_view);
        detectView.setAspectRatio(3,4);

        mStartTrack = (Button)findViewById(R.id.back);
        mStartTrack.setOnClickListener(this);
        mResetButton = (Button) findViewById(R.id.reset);
        mResetButton.setOnClickListener(this);

        imageView = (ImageView)findViewById(R.id.test_image);

        mSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mItems);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(arrayAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentTracker = mItems[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            mCameraSurfaceView.onResume();
            //addDetectView();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraSurfaceView.onPause();
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        mCameraSurfaceView.onDestroy();
        super.onDestroy();

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.back:
                mCameraSurfaceView.initTracker(mCurrentTracker);
                RectF rectF = detectView.getmRectF();
                rectF = new RectF(rectF.top, mCameraSurfaceView.getWidth() - rectF.right, rectF.bottom, mCameraSurfaceView.getWidth() - rectF.left);
                mCameraSurfaceView.setDetectCallback(new CameraSurfaceView.DetectCallback(){
                    @Override
                    public void onRectDetected(Rect2d rect2d) {
                        if(rect2d != null){
                            RectF rectF1 = new RectF();
                            rectF1.top =(float)(rect2d.tl().y);
                            rectF1.left =(float)(rect2d.tl().x);
                            rectF1.bottom =(float)(rect2d.br().y);
                            rectF1.right =(float)(rect2d.br().x);
                            rectF1 = new RectF( mCameraSurfaceView.getWidth() - rectF1.bottom, rectF1.left, mCameraSurfaceView.getWidth() - rectF1.top, rectF1.right);
                            if(detectView != null){
                                Log.i(TAG, "jerrypxiao startTracking rectF =" + rectF1.top + ","+ rectF1.left
                                        + ","+ rectF1.bottom + "," +rectF1.right);
                                detectView.setmDectedRectF(rectF1);
                            }
                        }
                    }

                    @Override
                    public void onDetectedBitmap(Bitmap bitmap) {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                    }
                });

                mCameraSurfaceView.startTracking(rectF);
                break;
            case R.id.reset:
                detectView.reset();
                mCameraSurfaceView.reset();
                mCameraSurfaceView.setDetectCallback(null);
                break;
        }
    }

}
