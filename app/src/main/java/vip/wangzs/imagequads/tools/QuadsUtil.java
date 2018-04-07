package vip.wangzs.imagequads.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vip.wangzs.imagequads.HeapPriorityQueue;

/**
 * Created by wangzs on 2018/4/2.
 */

public class QuadsUtil {
    public static final int MODE_RECT = 0;
    public static final int MODE_CIRCLE = 1;

    public static final int MODE = MODE_RECT;

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
    static final int LEAF_SIZE = 6;
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

        public Model init(String imgPath, int setW, int setH) {
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

        public void render(int maxDepth, Canvas canvas) {
            int dx = PADDING, dy = PADDING;
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawRect(0, 0, width * OUTPUT_SCALE, height * OUTPUT_SCALE, paint);

            List<Quad> leafNodes = root.getLeafNodes(maxDepth);

            for (Quad quad : leafNodes) {
                int l = quad.box.left * OUTPUT_SCALE + dx,
                        t = quad.box.top * OUTPUT_SCALE + dy,
                        r = quad.box.right * OUTPUT_SCALE - dx,
                        b = quad.box.bottom * OUTPUT_SCALE - dy;
                paint.setColor(quad.color.toColor());
                if (MODE == MODE_RECT) {
                    canvas.drawRect(l, t, r, b, paint);
                } else if (MODE == MODE_CIRCLE) {
                    int centerX = (l + r) / 2,
                            centerY = (b + t) / 2;
                    float radius = (r - l) / 2.f;
                    canvas.drawCircle(centerX, centerY, radius, paint);
                }
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
            children.add(tl);
            children.add(tr);
            children.add(bl);
            children.add(br);
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
                for (Quad child : children) {
                    List<Quad> leafNodes = child.getLeafNodes(maxDepth);
                    result.addAll(leafNodes);
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
