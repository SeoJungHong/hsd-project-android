package com.seojung.hsdproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by SeoJung on 16. 5. 30..
 */
public class SurfViewHeart extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "SurfViewHeart";
    private SurfaceHolder mSurfaceHolder;
    private Thread mThread;

    private Paint paint;
    private int screenW, screenH;
    private float X, Y;
    private Path path;
    private float TX;
    private boolean translate;

    public SurfViewHeart(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "SurfViewHeart is constructed");
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);
    }

    // SurfaceHolder.Callback implementation
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged (w=" + width + " , h=" + height + ")");
        screenW = width;
        screenH = height;
        X = 0;
        Y = screenH / 2;
        TX = 0;
        translate=false;

        path = new Path();
        path.moveTo(X, Y);

        paint = new Paint();
        paint.setColor(Color.argb(0xff, 0x99, 0x00, 0x00));
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setShadowLayer(7, 0, 0, Color.RED);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        for (;;){
            try{
                mThread.join(); // Thread 종료 기다리기
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(-TX, 0);
        X += 8;
        if (translate) {
            TX += 8;
        }
        path.lineTo(X, Y);
        if (X > screenW) {
            translate = true;
        }
        canvas.drawPath(path, paint);
        run();
    }

    Canvas canvas;

    @Override
    public void run() {
            synchronized(mSurfaceHolder){
                canvas = mSurfaceHolder.lockCanvas();
                postInvalidate();
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
    }

    public void setY(int y) {
        if (y < 0 || y > 300) {
            Log.e(TAG, "heart value - Wrong y value : " + y);
            y = 0;
        }
        Y = 700 - 2 * y;
    }
}
