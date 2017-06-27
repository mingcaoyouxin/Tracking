package com.tracking.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by jerrypxiao on 2017/6/8.
 */

public class NormalView extends View {
    private final static String TAG = "NormalView";
    private float startX;
    private float startY;
    private Paint mPaint;
    private Paint mPaint2;
    private RectF mRectF = new RectF();
    private RectF mDectedRectF;
    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;

    public NormalView(Context context) {
        this(context, null);
    }

    public NormalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mPaint = new Paint();
        mPaint.setColor(Color.YELLOW);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(3);

        mPaint2 = new Paint();
        mPaint2.setColor(Color.RED);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeJoin(Paint.Join.ROUND);
        mPaint2.setStrokeCap(Paint.Cap.ROUND);
        mPaint2.setStrokeWidth(3);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
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

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int event = motionEvent.getActionMasked();
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if(MotionEvent.ACTION_DOWN == event) {
            Log.i(TAG, "jerrypxiao x =" + x + ", y =" + y);
            startX = x;
            startY = y;
        }else if(MotionEvent.ACTION_MOVE == event){
            RectF rectF = new RectF(startX, startY, x, y);
            Log.i(TAG, "jerrypxiao move x =" + x + ", y =" + y);
            mRectF = rectF;
            invalidate();
        }else if(MotionEvent.ACTION_UP == event
                || MotionEvent.ACTION_CANCEL == event){

        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //RectF rectF = new RectF(0, 0, 100, 100);
        if (mDectedRectF == null) {
            canvas.drawRect(mRectF, mPaint);
        } else {
            canvas.drawRect(mDectedRectF, mPaint2);
        }
    }

    public RectF getmRectF(){
        return mRectF;
    }

    public void setmDectedRectF(RectF rectF){
        mDectedRectF = rectF;
        invalidate();
    }

    public void reset( ) {
        mRectF = new RectF(0, 0, 0, 0);
        mDectedRectF = null;
        invalidate();
    }
}
