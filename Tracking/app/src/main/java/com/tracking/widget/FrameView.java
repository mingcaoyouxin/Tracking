package com.tracking.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.tracking.R;

public class FrameView extends View {
    PointF mStartPoint;
    PointF mEndPoint;
    Paint mPaint;
    Paint mPaint2;
    Paint mPaint3;
    Paint mTextPaint;
    Paint mTextBackgroundPaint;
    RectF mRectF;
    public RectF mDrawRectF;//tracker 比例
    RectF mPredictRectF;
    float mRateX;
    float mRateY;
    CaluateViewCallBack mCallBack;
    int mCameraWidth;
    int mCameraHeight;
    public static final int CameraMode = 1;
    public static final int VideoMode = 2;

    public int mViewMode = CameraMode;

    Bitmap bmp;

    public FrameView(Context context) {
        super(context);
        init();
    }

    public FrameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public interface CaluateViewCallBack{
        public void onCaluate(RectF rectF);
    }

    public void setCameraSize(int cameraWidth,int cameraHeight){
        this.mCameraHeight = cameraHeight;
        this.mCameraWidth = cameraWidth;
    }

    public void setDrawCallback(CaluateViewCallBack callback){
        this.mCallBack = callback;
    }

    protected void init(){
        mStartPoint = new PointF();
        mEndPoint = new PointF();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.WHITE);

        mPaint2 = new Paint();
        mPaint2.setColor(Color.RED);
        mPaint2.setTextSize(24);
        mPaint2.setStrokeJoin(Paint.Join.ROUND);
        mPaint2.setStrokeCap(Paint.Cap.ROUND);
        mPaint2.setStrokeWidth(2);

        mPaint3 = new Paint();
        mPaint3.setColor(Color.parseColor("#D9FFFF"));
        mPaint3.setStyle(Paint.Style.STROKE);
        mPaint3.setStrokeWidth(4);

        mTextPaint = new Paint();
        mTextPaint.setTextSize(36);
        mTextPaint.setColor(Color.RED);
        mTextPaint.setStrokeWidth(2);

        mTextBackgroundPaint = new Paint();
        mTextBackgroundPaint.setColor(Color.parseColor("#4FD9FFFF"));
        mTextBackgroundPaint.setStrokeJoin(Paint.Join.ROUND);
        mTextBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.test_icon);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mRectF = null;
                mPredictRectF = null;
                if(mDrawRectF != null){
                    synchronized (mDrawRectF){
                        mDrawRectF = null;
                    }
                }
                mStartPoint.x =  event.getX();
                mStartPoint.y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mEndPoint.x = event.getX();
                mEndPoint.y = event.getY();
                caluateRectF();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mEndPoint.x = event.getX();
                mEndPoint.y = event.getY();
                caluateRectF();
                onCallBack();
                invalidate();
                break;
        }
        return true;
    }

    protected void caluateRectF(){
        float left,right,top,bottom;
        if(mStartPoint.x < mEndPoint.x){
            left = mStartPoint.x;
            right = mEndPoint.x;
        }else{
            left = mEndPoint.x;
            right = mStartPoint.x;
        }
        if(mStartPoint.y < mEndPoint.y){
            top = mStartPoint.y;
            bottom = mEndPoint.y;
        }else{
            top = mEndPoint.y;
            bottom = mStartPoint.y;
        }
        if(left == right || top == bottom){
            return;
        }
        mRateX = mCameraWidth*1.0f/this.getWidth();
        mRateY = mCameraHeight*1.0f/this.getHeight();
        mRectF = new RectF(left,top,right,bottom);
    }

    public void onCallBack(){
        if(mRectF == null){
            return;
        }
        if(mDrawRectF == null){
            mDrawRectF = new RectF();
        }
        if(mPredictRectF == null){
            mPredictRectF = new RectF();
        }
        synchronized (mDrawRectF){
            if(mViewMode == CameraMode){
                int viewHeight = getWidth();
                mDrawRectF.top = viewHeight - mRectF.right;
                mDrawRectF.left = mRectF.top;
                mDrawRectF.bottom = viewHeight - mRectF.left;
                mDrawRectF.right = mRectF.bottom;
            }else{
                mDrawRectF = mRectF;
            }
            if(mCallBack != null){
                mCallBack.onCaluate(mDrawRectF);
            }
        }
    }

    protected RectF getDrawRectF(){
        RectF rectF = new RectF();
        if(mViewMode == CameraMode){
            int viewHeight = getWidth();
            rectF.left = viewHeight - mDrawRectF.bottom;
            rectF.top = mDrawRectF.left;
            rectF.right = viewHeight - mDrawRectF.top;
            rectF.bottom = mDrawRectF.right;
        }else{
            rectF = mDrawRectF;
        }
        return rectF;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.WHITE);
        if(mDrawRectF != null){
            //canvas.drawRect(getDrawRectF(), mPaint);
            //drawBitmap(canvas, getDrawRectF().left, getDrawRectF().top);
            drawSpecialSelectedRect(canvas, getDrawRectF());
            if(mPredictRectF != null){
                mPaint.setColor(Color.BLUE);
                canvas.drawRect(mPredictRectF,mPaint);
            }
            return;
        }
        if(mRectF != null){
            canvas.drawRect(mRectF, mPaint);
            String dpStr = ""+ (int)mRectF.width() + "x" + (int)mRectF.height();
            canvas.drawText(dpStr, mRectF.left, mRectF.top, mPaint2);
        }
    }


    private void drawBitmap(Canvas canvas, float left, float top){
        canvas.drawBitmap(bmp, left, top, null);
    }

    private void drawSpecialSelectedRect(Canvas canvas, RectF rectF){
        if(rectF == null){
            return;
        }
        float length = 50.0f;
        //右上角
        canvas.drawLine(rectF.right - length, rectF.top, rectF.right, rectF.top, mPaint3);
        canvas.drawLine(rectF.right, rectF.top, rectF.right, rectF.top + length, mPaint3);
        //右下角
        canvas.drawLine(rectF.right, rectF.bottom - length, rectF.right, rectF.bottom, mPaint3);
        canvas.drawLine(rectF.right - length, rectF.bottom, rectF.right, rectF.bottom, mPaint3);
        //左上角
        canvas.drawLine(rectF.left, rectF.top, rectF.left + length, rectF.top, mPaint3);
        canvas.drawLine(rectF.left, rectF.top, rectF.left, rectF.top + length, mPaint3);
        //左下角
        canvas.drawLine(rectF.left, rectF.bottom - length, rectF.left, rectF.bottom, mPaint3);
        canvas.drawLine(rectF.left, rectF.bottom, rectF.left + length, rectF.bottom, mPaint3);

        //中心圈
        canvas.drawCircle(rectF.left + rectF.width()/2.0f, rectF.top + rectF.height()/2.0f, 5.0f, mPaint3);

        //canvas.drawRect(rectF, mPaint);
        //斜方45度线条
        canvas.drawLine(rectF.right, rectF.top, rectF.right + length * 2.0f, rectF.top - length * 2.0f, mPaint3);
        //画文字
        String dpStr = " It is Here! ";
        canvas.drawText(dpStr, rectF.right + length * 2.0f, rectF.top - length * 2.0f - 5.0f, mTextPaint);

        float textlength = getFontlength(mTextPaint, dpStr) + 6.0f;
        float textheight = getFontHeight(mTextPaint);

        //水平线条
        canvas.drawLine(rectF.right + length * 2.0f, rectF.top - length * 2.0f,
                rectF.right + length * 2.0f + textlength, rectF.top - length * 2.0f, mPaint3);

//        String dpStr = ""+ (int)mRectF.width() + "x" + (int)mRectF.height();
        //画文字的背景
        canvas.drawRect(rectF.right + length * 2.0f, rectF.top - length * 2.0f - textheight,
                rectF.right + length * 2.0f+ textlength , rectF.top - length * 2.0f, mTextBackgroundPaint);

    }


    /**
     * @return 返回指定笔和指定字符串的长度
     */
    public static float getFontlength(Paint paint, String str) {
        return paint.measureText(str);
    }
    /**
     * @return 返回指定笔的文字高度
     */
    public static float getFontHeight(Paint paint)  {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return fm.descent - fm.ascent;
    }
    /**
     * @return 返回指定笔离文字顶部的基准距离
     */
    public static float getFontLeading(Paint paint)  {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return fm.leading- fm.ascent;
    }
}
