package vip.wangzs.imagequads.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vip.wangzs.imagequads.HeapPriorityQueue;

/**
 * Created by wangzs on 2018/4/2.
 */

public class QuadsUtil {
    public static final int MODE_RECT = 0;          // 矩形
    public static final int MODE_CIRCLE = 1;        // 圆形
    public static final int MODE_OVAL = 2;          // 椭圆
    public static final int MODE_ROUND_RECT = 3;    // 圆角矩形
    public static final int MODE_HEX = 4;           // 正六边形

    /*
    ITERATIONS = 1024
    LEAF_SIZE = 4
    PADDING = 1
    FILL_COLOR = (0, 0, 0)
    SAVE_FRAMES = False
    ERROR_RATE = 0.5
    AREA_POWER = 0.25
    OUTPUT_SCALE = 1
     */
    static final int LEAF_SIZE = 8;
    static final int OUTPUT_SCALE = 1;
    static final int PADDING = 1;

    static class ValWeight {
        int value;
        float error;

        ValWeight(int v, float e) {
            value = v;
            error = e;
        }
    }

    /**
     * 从颜色直方图中获取均值rgb颜色
     */
    private static ValWeight weightedAverage(int[] hist, int index, int len) {
        int total = 1;
        for (int i = 0; i < len; ++i) {
            total += hist[i + index];
        }
        int value = 0;
        for (int i = 0; i < len; ++i) {
            value += i * hist[i + index];
        }
        value = value / total;

        float error = 0;
        for (int i = 0; i < len; ++i) {
            error += (value - i) * (value - i) * hist[i + index];
        }
        // 颜色的方差，error值越大说明颜色分布差异越大（即需要做分裂，细化颜色）
        error = (float) Math.sqrt(error / total);
        return new ValWeight(value, error);
    }

    /**
     * 计算rgb各个分量的均值颜色
     */
    private static float colorFromHistogram(int[] hist, IColor outColor) {
        ValWeight r = weightedAverage(hist, 0, 256);
        ValWeight g = weightedAverage(hist, 256, 256);
        ValWeight b = weightedAverage(hist, 512, 256);
        outColor.r = r.value;
        outColor.g = g.value;
        outColor.b = b.value;
        // rgb -> grayscale (即rgb各个error的权重)
        return r.error * 0.2989f + g.error * 0.5870f + b.error * 0.1140f;
    }

    public static class IColor {
        int r, g, b;

        IColor() {
        }

        public IColor(int r, int b, int g) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        int toColor() {
            return Color.rgb(r, g, b);
        }
    }

    public static class Box {
        int left, top, right, bottom;

        Box(int l, int t, int r, int b) {
            left = l;
            right = r;
            top = t;
            bottom = b;
        }

        public Box() {
        }
    }

    public static class Model {
        byte[] im;
        int width, height;
        Quad root;
        float errorSum;

        HeapPriorityQueue<HeapKV, HeapKV> heap = new HeapPriorityQueue<>();

        public Model() {
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Model init(String imgPath, int setW, int setH) {
            int[] wh = ImageUtil.getImageSize(imgPath);
            int originW = wh[0], originH = wh[1];
            if (originW > originH) {    // w > h
                setH = (int) (setW * originH / (1.f * originW));
            } else if (originW < originH) {
                setW = (int) (setH * originW / (1.f * originH));
            }
            Bitmap bitmap = ImageUtil.decodeBitmapFromPath(imgPath, setW, setH);
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            im = ImageUtil.bitmap2RGB(bitmap);
            bitmap.recycle();

            root = new Quad(this, new Box(0, 0, width, height), 0);
            errorSum = root.error * root.area;
            push(root);
            return this;
        }

        public List<Quad> quads() {
            List<Quad> result = new ArrayList<>();
            for (int i = 0; i < heap.size(); ++i) {
                result.add(heap.getValue(i).quad);
            }
            return result;
        }

        public float averageError() {
            return errorSum / (width * height);
        }

        void push(Quad quad) {
            // quad.error值越大，box中颜色差异越大，加符号后则值越小，堆插入会排到前面（即需要先做box的分裂）
            float score = (float) (-quad.error * (Math.pow(quad.area, 0.25)));
            HeapKV h = new HeapKV(quad.leaf, score, quad);
            heap.insert(h, h);
        }

        Quad pop() {
            Map.Entry<HeapKV, HeapKV> heapKVHeapKVEntry = heap.removeMin();
            if (heapKVHeapKVEntry != null) {
                return heapKVHeapKVEntry.getValue().quad;
            }
            return null;
        }

        public void split() {
            Quad quad = pop();
            if (quad != null) {
                errorSum -= quad.error * quad.area;
                List<Quad> children = quad.split();
                for (Quad child : children) {
                    push(child);
                    errorSum += child.error * child.area;
                }
            }
        }

        // 绘制图形
        private void drawGraphic(Box box, Canvas canvas, Paint paint, Path viewPath, int mode) {
            int l = box.left * OUTPUT_SCALE + PADDING,
                    t = box.top * OUTPUT_SCALE + PADDING,
                    r = box.right * OUTPUT_SCALE,
                    b = box.bottom * OUTPUT_SCALE;
            RectF rectF;
            int centerX, centerY;
            float radius;

            switch (mode) {
                case MODE_CIRCLE:   // 绘制圆形
                    centerX = (l + r) / 2;
                    centerY = (b + t) / 2;
                    radius = (r - l) / 2.f;
                    canvas.drawCircle(centerX, centerY, radius, paint);
                    break;

                case MODE_OVAL:
                    rectF = new RectF(l, t, r, b);
                    canvas.drawOval(rectF, paint);
                    break;

                case MODE_RECT: // 绘制正方形
                    canvas.drawRect(l, t, r, b, paint);
                    break;

                case MODE_ROUND_RECT:   // 绘制圆角矩形
                    rectF = new RectF(l, t, r, b);
                    canvas.drawRoundRect(rectF, LEAF_SIZE / 2, LEAF_SIZE / 2, paint);
                    break;

                case MODE_HEX:
                    centerX = (l + r) / 2;
                    centerY = (b + t) / 2;
                    radius = (r - l) / 2.f;
                    drawHex(centerX, centerY, radius, canvas, paint, viewPath);
                    break;

                default:
                    canvas.drawRect(l, t, r, b, paint);
                    break;
            }
        }

        static float[] cosArr = {(float) Math.cos(0), (float) Math.cos(Math.PI / 3),
                (float) Math.cos(2 * Math.PI / 3), (float) Math.cos(Math.PI),
                (float) Math.cos(4 * Math.PI / 3), (float) Math.cos(5 * Math.PI / 3)};

        static float[] sinArr = {(float) Math.sin(0), (float) Math.sin(Math.PI / 3),
                (float) Math.sin(2 * Math.PI / 3), (float) Math.sin(Math.PI),
                (float) Math.sin(4 * Math.PI / 3), (float) Math.sin(5 * Math.PI / 3)};

        private void drawHex(int centerX, int centerY, float radius, Canvas canvas, Paint paint, Path viewPath) {
            viewPath.reset();
            for (int i = 0; i < 6; i++) {
                if (i == 0) {
                    viewPath.moveTo(centerX + radius * cosArr[i],
                            centerY + radius * sinArr[i]);
                } else {
                    viewPath.lineTo(centerX + radius * cosArr[i],
                            centerY + radius * sinArr[i]);
                }
            }
            viewPath.close();
            canvas.drawPath(viewPath, paint);
        }

        public void render(int maxDepth, Canvas canvas, int mode, int bgColor) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Path viewPath = new Path();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(bgColor);
            canvas.drawRect(0, 0, width * OUTPUT_SCALE, height * OUTPUT_SCALE, paint);

            List<Quad> leafNodes = root.getLeafNodes(maxDepth);

            for (Quad quad : leafNodes) {
                paint.setColor(quad.color.toColor());
                drawGraphic(quad.box, canvas, paint, viewPath, mode);
            }
        }
    }

    public static class Quad {
        Model model;
        Box box;
        int depth;
        int[] hist;
        public IColor color;
        float error;
        int leaf;    // 0 or 1
        int area;
        List<Quad> children;

        Quad(Model model, Box box, int depth) {
            this.model = model;
            this.box = box;
            this.depth = depth;
            this.hist = ImageUtil.histogram(
                    ImageUtil.crop(box.left, box.top, box.right, box.bottom, this.model.im, this.model.width)
            );
            this.color = new IColor();
            this.error = colorFromHistogram(this.hist, this.color);
            this.leaf = isLeaf();
            this.area = computeArea();
            children = new ArrayList<>();
        }

        int isLeaf() {
            int l = box.left, t = box.top, r = box.right, b = box.bottom;
            return (r - l <= LEAF_SIZE | b - t <= LEAF_SIZE) ? 1 : 0;
        }

        int computeArea() {
            return (box.right - box.left) * (box.bottom - box.top);
        }

        List<Quad> split() {
            if (isLeaf() == 1) {    // 达到leaf最小长度时停止分裂
                return children;
            }

            int l = box.left, t = box.top, r = box.right, b = box.bottom;
            int lr = (l + r) / 2;
            int tb = (b + t) / 2;

            depth += 1;
            Quad tl = new Quad(model, new Box(l, t, lr, tb), depth);
            Quad tr = new Quad(model, new Box(lr, t, r, tb), depth);
            Quad bl = new Quad(model, new Box(l, tb, lr, b), depth);
            Quad br = new Quad(model, new Box(lr, tb, r, b), depth);
            synchronized (this) {
                children.add(tl);
                children.add(tr);
                children.add(bl);
                children.add(br);
            }
            return children;
        }

        List<Quad> getLeafNodes(int maxDepth) {
            ArrayList<Quad> result = new ArrayList<>();
            if (children.isEmpty()) {
                result.add(this);
            }
            if (maxDepth != -1 && depth >= maxDepth) {
                result.add(this);
            } else {
                synchronized (this) {
                    for (Quad child : children) {
                        List<Quad> leafNodes = child.getLeafNodes(maxDepth);
                        result.addAll(leafNodes);
                    }
                }

            }
            return result;
        }
    }

    static class HeapKV implements Comparable<HeapKV> {
        int leaf;
        float score;
        Quad quad;

        HeapKV(int l, float s, Quad q) {
            leaf = l;
            score = s;
            quad = q;
        }

        @Override
        public int compareTo(@NonNull HeapKV heapKey) {
            if (leaf < heapKey.leaf) {
                return -1;
            } else if (leaf == heapKey.leaf) {
                if (score < heapKey.score) {
                    return -1;
                } else if (score == heapKey.score) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return 1;
            }
        }
    }
}
