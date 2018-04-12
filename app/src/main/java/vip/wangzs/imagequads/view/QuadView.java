package vip.wangzs.imagequads.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import vip.wangzs.imagequads.tools.QuadsUtil;

/**
 * Created by wangzs on 2018/4/3.
 */

public class QuadView extends View {
    public static final int STATUS_RUN = 0;
    public static final int STATUS_PAUSE = 1;
    public static final int STATUS_STOP = 2;

    private final static int MAX_SPLIT_CNT = 2048;

    private Handler handler;
    private QuadsUtil.Model model;
    private int status = STATUS_STOP;
    private int splitCnt = 0;
    private OnStatusChange listener;
    private int drawMode = QuadsUtil.MODE_ROUND_RECT;
    private int bgColor = Color.BLACK;

    public QuadView(Context context) {
        super(context);
        init();
    }

    public QuadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public QuadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                invalidate();
            }
        };
    }

    public void doConfig(int mode, int bgColor) {
        this.drawMode = mode;
        this.bgColor = bgColor;
    }

    public void startInit(QuadsUtil.Model model, OnStatusChange statusChangeListener) {
        splitCnt = 0;
        status = STATUS_RUN;
        this.model = model;
        this.listener = statusChangeListener;
        if (this.model.getWidth() != this.model.getHeight()
                && this.drawMode == QuadsUtil.MODE_CIRCLE) {
            this.drawMode = QuadsUtil.MODE_OVAL;
        }

        startRender();
    }

    public int startOrPause() {
        if (status == STATUS_PAUSE) {
            status = STATUS_RUN;
            startRender();
        } else if (status == STATUS_RUN) {
            status = STATUS_PAUSE;
        }
        return status;
    }

    public synchronized void stop() {
        status = STATUS_STOP;
        listener.onSplit(-1);
    }

    public void saveToFile(File outFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outFile);
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
        bitmap.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (model != null) {
            widthMeasureSpec = model.getWidth();
            heightMeasureSpec = model.getHeight();
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            int wMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            int hMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
            super.onMeasure(wMeasureSpec, hMeasureSpec);
        } else {
            int finalMeasureSpec;
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);


            int size;
            if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
                size = widthSize;
            } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
                size = heightSize;
            } else {
                size = widthSize < heightSize ? widthSize : heightSize;
            }

            finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            super.onMeasure(finalMeasureSpec, finalMeasureSpec);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model != null) {
            model.render(-1, canvas, drawMode, bgColor);
        }
    }

    private void startRender() {
        if (model == null) {
            Log.e("QuadView", "Should call startInit interface!");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (status != STATUS_RUN) {
                        break;
                    } else if (splitCnt > MAX_SPLIT_CNT) {
                        stop();
                        handler.sendEmptyMessage(1);
                        listener.onMaxSplitStop();
                    } else {
                        split();
                    }
                }
            }
        }).start();
    }

    private void split() {
        synchronized (this) {
            splitCnt++;
            model.split();
            handler.sendEmptyMessage(1);
            listener.onSplit(splitCnt);
        }
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface OnStatusChange {
        void onMaxSplitStop();

        void onSplit(int cnt);
    }
}
