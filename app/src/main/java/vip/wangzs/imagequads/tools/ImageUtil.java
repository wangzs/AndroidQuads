package vip.wangzs.imagequads.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by wangzs on 2018/4/2.
 */

public class ImageUtil {

    public static Bitmap decodeBitmapFromResource(Context context, int imgResId, int reqW, int reqH) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateSampleSize(options, reqW, reqH);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(context.getResources(), imgResId, options);
    }

    // 计算合适的采样率(当然这里还可以自己定义计算规则)，reqWidth为期望的图片大小，单位是px
    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        while ((halfWidth / inSampleSize) >= reqWidth && (halfHeight / inSampleSize) >= reqHeight) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromPath(String imgPath, int reqW, int reqH) {
        Bitmap out = null;
        try {
            Bitmap tmp = BitmapFactory.decodeFile(imgPath);
            out = Bitmap.createScaledBitmap(tmp, reqW, reqH, false);
            tmp.recycle();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return out;
    }

    public static Bitmap decodeBitmapFromPath(String imgPath) {
        Log.d("Bitmap", "load image path: " + imgPath);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(imgPath);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static byte[] bitmap2RGB(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  // 返回可用于储存此位图像素的最小字节数

        ByteBuffer buffer = ByteBuffer.allocate(bytes); // 使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {

            pixels[i * 3] = rgba[i * 4];            // R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    // G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];    // B
        }

        return pixels;
    }

    public static int[] histogram(byte[] pixels) {
        int[] hist = new int[768];
        int count = pixels.length;
        for (int i = 0; i < count; i += 3) {
            int r = pixels[i] & 0xFF;
            int g = pixels[i + 1] & 0xFF;
            int b = pixels[i + 2] & 0xFF;
            hist[r]++;
            hist[g + 256]++;
            hist[b + 512]++;
        }
        return hist;
    }

    public static byte[] crop(int left, int top, int right, int bottom, byte[] pixels, int width) {
        int newWidth = right - left;
        int newHeight = bottom - top;
        int len = newWidth * newHeight * 3;
        byte[] cropPixels = new byte[len];
        for (int y = 0; y < newHeight; ++y) {
            for (int x = 0; x < newWidth; ++x) {
                cropPixels[y * newWidth * 3 + x * 3] = pixels[((y + top) * width + left) * 3 + x * 3];
                cropPixels[y * newWidth * 3 + x * 3 + 1] = pixels[((y + top) * width + left) * 3 + x * 3 + 1];
                cropPixels[y * newWidth * 3 + x * 3 + 2] = pixels[((y + top) * width + left) * 3 + x * 3 + 2];
            }
        }
        return cropPixels;
    }
}
