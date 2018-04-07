package vip.wangzs.imagequads.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

    private final static int MAX_SPLIT_CNT = 4096;

    private Handler handler;
    private QuadsUtil.Model model;
    private int status = STATUS_STOP;
    private int splitCnt = 0;
    private OnStatusChange listener;

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

    public void startInit(QuadsUtil.Model model, OnStatusChange statusChangeListener) {
        status = STATUS_RUN;
        this.model = model;
        this.listener = statusChangeListener;
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
    protected void onDraw(Canvas canvas) {
        if (model != null) {
            model.render(-1, canvas);
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
