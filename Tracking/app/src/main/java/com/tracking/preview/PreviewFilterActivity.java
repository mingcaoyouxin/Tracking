package com.tracking.preview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.muzhi.camerasdk.BaseActivity;
import com.muzhi.camerasdk.adapter.Filter_Effect_Adapter;
import com.muzhi.camerasdk.adapter.Filter_Sticker_Adapter;
import com.muzhi.camerasdk.library.filter.GPUImageFilter;
import com.muzhi.camerasdk.library.filter.util.ImageFilterTools;
import com.muzhi.camerasdk.library.views.HorizontalListView;
import com.muzhi.camerasdk.model.CameraSdkParameterInfo;
import com.muzhi.camerasdk.model.Constants;
import com.muzhi.camerasdk.model.Filter_Effect_Info;
import com.muzhi.camerasdk.model.Filter_Sticker_Info;
import com.muzhi.camerasdk.utils.FilterUtils;
import com.tracking.R;
import com.tracking.widget.CameraTextureView;
import com.tracking.widget.ConfirmationDialogFragment;
import com.tracking.widget.FrameView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;

import java.util.ArrayList;

import static com.muzhi.camerasdk.library.filter.util.ImageFilterTools.FilterType.I_1977;
import static com.muzhi.camerasdk.library.filter.util.ImageFilterTools.FilterType.I_AMARO;
import static com.muzhi.camerasdk.library.filter.util.ImageFilterTools.FilterType.I_BRANNAN;
import static com.muzhi.camerasdk.library.filter.util.ImageFilterTools.FilterType.I_EARLYBIRD;

public class PreviewFilterActivity extends BaseActivity implements View.OnClickListener{
	private final static String TAG = "PreviewFilterActivity";
	private static final int REQUEST_CAMERA_PERMISSION = 1;
	private static final String FRAGMENT_DIALOG = "dialog";

	private CameraSdkParameterInfo mCameraSdkParameterInfo=new CameraSdkParameterInfo();
	private HorizontalListView effect_listview, sticker_listview;
	private TextView tab_effect, tab_sticker,txt_cropper,txt_enhance,txt_graffiti, txt_Fps, txt_cost_time;

	private Filter_Effect_Adapter eAdapter;
	private Filter_Sticker_Adapter sAdapter;

	private ArrayList<Filter_Effect_Info> effect_list=new ArrayList<Filter_Effect_Info>(); //特效
	private ArrayList<Filter_Sticker_Info> stickerList = new ArrayList<Filter_Sticker_Info>();

	public static Filter_Sticker_Info mSticker = null; // 从贴纸库过来的贴纸
	private CameraTextureView mCameraTextureView;
	private FrameView mFramView;
	private Boolean mIsInitTrack = false;
	private RectF currentRectF;
	public String mType = "CMT";

	//显示的View的和实际相机高宽的y方向，x方向的比率
	private float mRateY;
	private float mRateX;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.preview_filter_activity);
		initView();
		initEvent();
		initData();
	}

	private void initView() {
		tab_effect = (TextView) findViewById(R.id.txt_effect);
		tab_sticker = (TextView) findViewById(R.id.txt_sticker);
		txt_cropper = (TextView) findViewById(R.id.txt_cropper);
		txt_enhance = (TextView) findViewById(R.id.txt_enhance);
		txt_graffiti = (TextView) findViewById(R.id.txt_graffiti);
		txt_Fps = (TextView) findViewById(R.id.txt_fps);
		txt_cost_time = (TextView) findViewById(R.id.txt_cost_time);

		effect_listview = (HorizontalListView) findViewById(R.id.effect_listview);
		sticker_listview = (HorizontalListView) findViewById(R.id.sticker_listview);

		mCameraTextureView = (CameraTextureView)findViewById(R.id.texture_cameraview);
		mFramView = (FrameView)findViewById(R.id.obj_frameview);

	}

	private void initEvent(){
		mCameraTextureView.setCameraDataListener(cameraDatalistener);
		tab_effect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				effect_listview.setVisibility(View.VISIBLE);
				sticker_listview.setVisibility(View.INVISIBLE);
				tab_effect.setTextColor(getResources().getColor(com.muzhi.camerasdk.R.color.camerasdk_txt_selected));
				tab_sticker.setTextColor(getResources().getColor(com.muzhi.camerasdk.R.color.camerasdk_txt_normal));
			}
		});
		tab_sticker.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				effect_listview.setVisibility(View.INVISIBLE);
				sticker_listview.setVisibility(View.VISIBLE);
				tab_effect.setTextColor(getResources().getColor(com.muzhi.camerasdk.R.color.camerasdk_txt_normal));
				tab_sticker.setTextColor(getResources().getColor(com.muzhi.camerasdk.R.color.camerasdk_txt_selected));
			}
		});
		txt_cropper.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 裁剪图片
				/*Constants.bitmap=((EfectFragment)fragments.get(current)).getCurrentBitMap();
				Intent intent = new Intent();
				intent.setClassName(getApplication(), "com.muzhi.camerasdk.CutActivity");				
				startActivityForResult(intent,Constants.RequestCode_Croper);*/
			}
		});
		txt_enhance.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO 图片增强
				/*Constants.bitmap=((EfectFragment)fragments.get(current)).getCurrentBitMap();
				Intent intent = new Intent();
				intent.setClassName(getApplication(), "com.muzhi.camerasdk.PhotoEnhanceActivity");				
				startActivityForResult(intent,Constants.RequestCode_Croper);*/
			}
		});
		txt_graffiti.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO 涂鸦
				/*Constants.bitmap=((EfectFragment)fragments.get(current)).getCurrentBitMap();
				Intent intent = new Intent();
				intent.putExtra("path", imageList.get(0));
				intent.setClassName(getApplication(), "com.muzhi.camerasdk.GraffitiActivity");				
				startActivityForResult(intent,Constants.RequestCode_Croper);*/
			}
		});
		
		effect_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				eAdapter.setSelectItem(arg2);

				final int tmpint = arg2;
				final int tmpitem = arg1.getWidth();
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						effect_listview.scrollTo(tmpitem * (tmpint - 1) - tmpitem / 4);
					}
				}, 200);

				Filter_Effect_Info info= effect_list.get(arg2);
				GPUImageFilter filter = ImageFilterTools.createFilterForType(mContext,info.getFilterType());
				//((EfectFragment)fragments.get(current)).addEffect(filter);
				//{"KCF", "TLD", "MIL", "BOOSTING", "GOTURN", "MEDIANFLOW"};
				if(info.getFilterType() == I_1977){
					mType = "KCF";
					mIsInitTrack = false;
				}else if(info.getFilterType() == I_AMARO){
					mType = "TLD";
					mIsInitTrack = false;
				}else if(info.getFilterType() == I_BRANNAN){
					mType = "MIL";
					mIsInitTrack = false;
				}else if(info.getFilterType() == I_EARLYBIRD){
					mType = "MEDIANFLOW";
					mIsInitTrack = false;
				}else if(info.getFilterType() == ImageFilterTools.FilterType.I_HUDSON){
					mType = "BOOSTING";
					mIsInitTrack = false;
				}else if(info.getFilterType() == ImageFilterTools.FilterType.I_NASHVILLE){
					mType = "CMT";
					mIsInitTrack = false;
				}
			}
		});

		sticker_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				
				String path=stickerList.get(arg2).getLocal_path();
				int drawableId=stickerList.get(arg2).getDrawableId();
				//((EfectFragment)fragments.get(current)).addSticker(drawableId, path);
				
			}
		});
		
	}

	private void initData(){
		
		effect_list=FilterUtils.getEffectList();
		stickerList=FilterUtils.getStickerList();

		eAdapter = new Filter_Effect_Adapter(this,effect_list);
		sAdapter = new Filter_Sticker_Adapter(this, stickerList);

		effect_listview.setAdapter(eAdapter);
		sticker_listview.setAdapter(sAdapter);
		
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
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// Low Memory Unity
	@Override public void onLowMemory()
	{
		super.onLowMemory();
	}

	// Trim Memory Unity
	@Override public void onTrimMemory(int level)
	{
		super.onTrimMemory(level);
		if (level == TRIM_MEMORY_RUNNING_CRITICAL)
		{
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.back:
				break;
		}
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i("PreviewActivity", "OpenCV loaded successfully");
					//mCameraSurfaceView.initTracker();
					System.loadLibrary("OpenCV");
				}
				break;
				default: {
					super.onManagerConnected(status);
				}
				break;
			}
		}
	};


	private CameraTextureView.CameraDataListener cameraDatalistener = new CameraTextureView.CameraDataListener() {
		@Override
		public void onGetFrame(byte[] data) {
			if (mFramView.mDrawRectF == null) {
				return;
			}
			mCurrentData = data;
			if(mTrackThread == null || !mTrackThread.isAlive()){
				mTrackThread = new TrackThread();
				mTrackThread.start();
			}
		}

		@Override
		public void resetLayout(int width, int height, int cameraWidth, int cameraHeight) {
			Log.i(TAG, "width = " + width + ", height =" + height);
			mFramView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
			mFramView.setCameraSize(cameraWidth, cameraHeight);
			mFramView.setDrawCallback(new FrameView.CaluateViewCallBack() {
				@Override
				public void onCaluate(RectF rectF) {
					mIsInitTrack = false;
					currentRectF = new RectF(rectF);
				}
			});

			mRateY = width * 1.0f / mCameraTextureView.mCameraHeight;
			mRateX = height * 1.0f / mCameraTextureView.mCameraWidth;
		}

		@Override
		public void onGetFPS(float fps) {
			txt_Fps.setText("绘制FPS：" + fps);
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Constants.RequestCode_Croper) {
			//截图返回
			//((EfectFragment)fragments.get(current)).setBitMap();
		}
		else if(resultCode == Constants.RequestCode_Sticker){
			if(data!=null){
				Filter_Sticker_Info info=(Filter_Sticker_Info)data.getSerializableExtra("info");
				//((EfectFragment)fragments.get(current)).addSticker(0, info.getImage());
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	byte[] mCurrentData;
	Handler mHandler = new Handler(Looper.getMainLooper());
	TrackThread mTrackThread;
	private Tracker mTracker;


	class TrackThread extends Thread{
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			if (mFramView.mDrawRectF == null || mCurrentData == null) {
				return;
			}
			synchronized (mFramView.mDrawRectF) {
				if(!mCameraTextureView.isAvailable()){
					return;
				}
				float rateY = mFramView.getWidth() * 1.0f / mCameraTextureView.mCameraHeight;
				float rateX = mFramView.getHeight() * 1.0f / mCameraTextureView.mCameraWidth;

				Log.i(TAG, "rateY = " + rateY + ", mrateY = " + mRateY);
				RectF rectF = new RectF(currentRectF);
				rectF.left = rectF.left / mRateX;
				rectF.right = rectF.right / mRateX;
				rectF.top = rectF.top / mRateY;
				rectF.bottom = rectF.bottom / mRateY;

				if((rectF.left + rectF.width()) > mCameraTextureView.mCameraWidth){
					rectF.right = mCameraTextureView.mCameraWidth;
				}

				if((rectF.top + rectF.height()) > mCameraTextureView.mCameraHeight){
					rectF.bottom = mCameraTextureView.mCameraHeight;
				}

/*				Log.i(TAG, "rectF.left = " + rectF.left + ", rectF.right = "
						+ rectF.right + ", rectF.top = " + rectF.top + ", rectF.bottom = " + rectF.bottom);

				Log.i(TAG, "mCameraTextureView.mCameraHeight = " + mCameraTextureView.mCameraHeight
						+ ", mCameraTextureView.mCameraWidth = " + mCameraTextureView.mCameraWidth);*/

				Mat frameMat = new Mat(mCameraTextureView.mCameraHeight + mCameraTextureView.mCameraHeight/2, mCameraTextureView.mCameraWidth, CvType.CV_8UC1);
				frameMat.put(0,0, mCurrentData);
				Mat rgbMat = new Mat();
				Imgproc.cvtColor(frameMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
				Mat cvt3Mat = new Mat(rgbMat.size(), CvType.CV_8UC3);
				Imgproc.cvtColor(rgbMat, cvt3Mat, Imgproc.COLOR_RGBA2RGB, 3);
				Mat graymat = frameMat.submat(0, mCameraTextureView.mCameraHeight,
						0,  mCameraTextureView.mCameraWidth);

				if(mType.equals("CMT")){
					if (!mIsInitTrack) {
						TrackerManager.newInstance().OpenCMT(graymat.getNativeObjAddr(), rgbMat.getNativeObjAddr(),
								(long)rectF.left, (long)rectF.top, (long)rectF.width(), (long)rectF.height());
						mIsInitTrack = true;
					} else {

						long startTime = System.currentTimeMillis();
						try {
							TrackerManager.newInstance().ProcessCMT(graymat.getNativeObjAddr(), rgbMat.getNativeObjAddr());
						}catch (Exception e){
							e.printStackTrace();
						}
						long endTime = System.currentTimeMillis();
						final long costTime = (endTime - startTime);
						mFramView.post(new Runnable() {
							@Override
							public void run() {
								if (txt_cost_time != null) {
									txt_cost_time.setText("检测时间 ：" + costTime + "ms");
								}
							}
						});
						double px = (double) rgbMat.width() / (double) rgbMat.width();
						double py = (double) rgbMat.height() / (double) rgbMat.height();

						int[] l = TrackerManager.newInstance().CMTgetRect();
						if (TrackerManager.newInstance().CMTisTrackValid() && l != null&& mFramView.mDrawRectF != null) {

							/*for(int i=0; i<l.length; i++){
								Log.e(TAG, "jerrypxiao [" + i + "] =, " + l[i]);
							}*/

							Point topLeft = new Point(l[2] * px, l[3] * py);
							//Point bottomLeft = new Point(l[2] * px, l[3] * py);
							Point bottomRight = new Point(l[6] * px, l[7] * py);
							//Point topRight = new Point(l[6] * px, l[7] * py);

							final RectF resultRectF = new RectF((float)topLeft.x * mRateX,
																(float) topLeft.y * mRateY,
																(float) bottomRight.x * mRateX,
																(float) bottomRight.y * mRateY);
							mFramView.post(new Runnable() {
								@Override
								public void run() {
									mFramView.resetDrawRectF(resultRectF);
								}
							});
						}else{
							mFramView.needDraw = false;
							mFramView.postInvalidate();
						}
					}
				}else {

					if (!mIsInitTrack) {
					/*TrackerManager.newInstance().openTrack(mCurrentData, TrackerManager.TYPE_NV21,(int) rectF.left, (int) rectF.top,
							(int) rectF.width(), (int) rectF.height(), mCameraTextureView.mCameraWidth, mCameraTextureView.mCameraHeight);*/

						mTracker = Tracker.create(mType);
						Rect2d rect2d = new Rect2d(rectF.left, rectF.top, rectF.width(), rectF.height());
						if (mTracker != null) {
							mTracker.init(cvt3Mat, rect2d);
						}
						mIsInitTrack = true;
					} else {
					/*int[] cmtData = TrackerManager.newInstance().processTrack(mCurrentData,
							TrackerManager.TYPE_NV21,mCameraTextureView.mCameraWidth, mCameraTextureView.mCameraHeight);*/
						Rect2d currentRect2d = new Rect2d();
						long startTime = System.currentTimeMillis();
						if (mTracker != null) {
							mTracker.update(cvt3Mat, currentRect2d);
						}
						long endTime = System.currentTimeMillis();

						final long costTime = (endTime - startTime);
						mFramView.post(new Runnable() {
							@Override
							public void run() {
								if (txt_cost_time != null) {
									txt_cost_time.setText("检测时间 ：" + costTime + "ms");
								}
							}
						});
						if (currentRect2d != null && mFramView.mDrawRectF != null) {
							final RectF resultRectF = new RectF((float) currentRect2d.tl().x * mRateX,
																(float) currentRect2d.tl().y * mRateY,
																(float) currentRect2d.br().x * mRateX,
																(float) currentRect2d.br().y * mRateY);
							mFramView.post(new Runnable() {
								@Override
								public void run() {
									mFramView.resetDrawRectF(resultRectF);
								}
							});
						}
					}

				}



			}
		}
	}


	static final int WIDTH = 400 ;//240;// 320;
	static final int HEIGHT =240;// 135;// ;//240;0;
	public Mat Reduce(Mat m) {
		Mat dst = new Mat();
		Imgproc.resize(m, dst, new org.opencv.core.Size(WIDTH, HEIGHT));
		return dst;
	}

	Mat ReduceColor(Mat m) {
		Mat dst = new Mat();
		Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(m, bmp);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);
		Utils.bitmapToMat(bmp2, dst);
		// Imgproc.resize(m, dst, new Size(WIDTH,HEIGHT), 0, 0,
		// Imgproc.INTER_CUBIC);
		return dst;
	}

}
