package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayDeque;

/** Pixel grid for drawing small icons. Templates are loaded as real pixels. */
public class PixelCanvasView extends View {
    public static final int PENCIL = 0, ERASER = 1, FILL = 2, PICK = 3;
    private static final int MAX_UNDO = 40;

    public interface Listener {
        void colorPicked(int color);
    }

    private int gw = 16, gh = 16;
    private int[] pixels = new int[16 * 16];
    private int tool = PENCIL;
    private int color = Color.WHITE;
    private Listener listener;
    private final ArrayDeque<int[]> undo = new ArrayDeque<>();
    private final Paint paint = new Paint();
    private float cell, left, top;
    private float zoom = 1f, panX, panY;
    private boolean showGrid = true;
    private boolean preview;
    private Runnable onPreviewTap;
    private float downX, downY;
    private boolean strokeActive, gesture;
    private float lastFx, lastFy;
    private final ScaleGestureDetector scaleDetector;

    public PixelCanvasView(Context context) {
        super(context);
        paint.setAntiAlias(false);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                zoomAround(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }
        });
    }

    public void setShowGrid(boolean on) {
        showGrid = on;
        invalidate();
    }

    public float getZoom() { return zoom; }

    /** Preview shows only the drawing: no grid, no checkerboard, no drawing. A tap calls onTap. */
    public void setPreview(boolean on, Runnable onTap) {
        preview = on;
        onPreviewTap = onTap;
        invalidate();
    }

    public void zoomBy(float factor) {
        zoomAround(factor, getWidth() / 2f, getHeight() / 2f);
    }

    public void resetZoom() {
        zoom = 1f;
        panX = 0;
        panY = 0;
        invalidate();
    }

    /** Zooms keeping the pixel under (fx, fy) in place, so pinching feels like holding the drawing. */
    private void zoomAround(float factor, float fx, float fy) {
        layout();
        float gx = (fx - left) / cell, gy = (fy - top) / cell;
        zoom = Math.max(1f, Math.min(32f, zoom * factor));
        float base = baseCell();
        float newCell = base * zoom;
        panX = fx - gx * newCell - (getWidth() - gw * newCell) / 2f;
        panY = fy - gy * newCell - (getHeight() - gh * newCell) / 2f;
        invalidate();
    }

    private float baseCell() {
        return Math.min(getWidth() / (float) gw, getHeight() / (float) gh);
    }

    /** Computes the size and position of the grid for the current zoom and keeps it inside the view. */
    private void layout() {
        cell = baseCell() * zoom;
        float maxX = Math.max(0f, (gw * cell - getWidth()) / 2f), maxY = Math.max(0f, (gh * cell - getHeight()) / 2f);
        panX = Math.max(-maxX, Math.min(maxX, panX));
        panY = Math.max(-maxY, Math.min(maxY, panY));
        left = (getWidth() - gw * cell) / 2f + panX;
        top = (getHeight() - gh * cell) / 2f + panY;
    }

    public void setListener(Listener l) { listener = l; }
    public void setTool(int t) { tool = t; }
    public void setColor(int c) { color = c | 0xFF000000; }
    public int getColor() { return color; }
    public int getGridWidth() { return gw; }
    public int getGridHeight() { return gh; }

    public void setGridSize(int w, int h) {
        gw = w;
        gh = h;
        pixels = new int[w * h];
        undo.clear();
        zoom = 1f;
        panX = 0;
        panY = 0;
        requestLayout();
        invalidate();
    }

    public void clear() {
        pushUndo();
        pixels = new int[gw * gh];
        invalidate();
    }

    public void undo() {
        if(undo.isEmpty()) return;
        pixels = undo.pop();
        invalidate();
    }

    /** Replaces the drawing with the bitmap. The bitmap becomes the grid, so a template is editable pixel by pixel. */
    public void load(Bitmap bmp) {
        setGridSize(bmp.getWidth(), bmp.getHeight());
        bmp.getPixels(pixels, 0, gw, 0, 0, gw, gh);
    }

    private void pushUndo() {
        if(undo.size() >= MAX_UNDO) undo.removeLast();
        undo.push(pixels.clone());
    }

    /** The finished image. */
    public Bitmap toBitmap() {
        Bitmap bmp = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, gw, 0, 0, gw, gh);
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        layout();
        int x0 = Math.max(0, (int) Math.floor(-left / cell)), x1 = Math.min(gw, (int) Math.ceil((getWidth() - left) / cell));
        int y0 = Math.max(0, (int) Math.floor(-top / cell)), y1 = Math.min(gh, (int) Math.ceil((getHeight() - top) / cell));
        for(int y = y0; y < y1; y++) {
            for(int x = x0; x < x1; x++) {
                int p = pixels[y * gw + x];
                if(p == 0) {
                    if(preview) continue;
                    p = ((x + y) & 1) == 0 ? 0xFF4A4A4A : 0xFF3C3C3C;
                }
                paint.setColor(p);
                canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell + 0.5f, top + (y + 1) * cell + 0.5f, paint);
            }
        }
        if(preview || !showGrid || cell < 4f) return;
        paint.setColor(0x33FFFFFF);
        paint.setStrokeWidth(1f);
        for(int i = x0; i <= x1; i++) canvas.drawLine(left + i * cell, top + y0 * cell, left + i * cell, top + y1 * cell, paint);
        for(int i = y0; i <= y1; i++) canvas.drawLine(left + x0 * cell, top + i * cell, left + x1 * cell, top + i * cell, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(cell <= 0) return true;
        scaleDetector.onTouchEvent(e);
        int action = e.getActionMasked();
        if(action == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
        }
        if(action == MotionEvent.ACTION_POINTER_DOWN) {
            if(strokeActive) {
                undo();
                strokeActive = false;
            }
            gesture = true;
            lastFx = focusX(e);
            lastFy = focusY(e);
            return true;
        }
        if(e.getPointerCount() >= 2 && action == MotionEvent.ACTION_MOVE) {
            float fx = focusX(e), fy = focusY(e);
            panX += fx - lastFx;
            panY += fy - lastFy;
            lastFx = fx;
            lastFy = fy;
            invalidate();
            return true;
        }
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if(preview && action == MotionEvent.ACTION_UP && !gesture && onPreviewTap != null
                    && Math.hypot(e.getX() - downX, e.getY() - downY) < 24) {
                onPreviewTap.run();
            }
            strokeActive = false;
            gesture = false;
            return true;
        }
        if(gesture || preview) return true;
        int x = (int) Math.floor((e.getX() - left) / cell), y = (int) Math.floor((e.getY() - top) / cell);
        boolean inside = x >= 0 && y >= 0 && x < gw && y < gh;
        if(action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if(!inside) return true;
            if(tool == PICK) {
                int p = pixels[y * gw + x];
                if(p != 0) { color = p; if(listener != null) listener.colorPicked(p); }
                return true;
            }
            pushUndo();
            strokeActive = true;
            if(tool == FILL) { fill(x, y); strokeActive = false; return true; }
        }
        if(inside && strokeActive && (tool == PENCIL || tool == ERASER)
                && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
            pixels[y * gw + x] = tool == PENCIL ? color : 0;
            invalidate();
        }
        return true;
    }

    private static float focusX(MotionEvent e) {
        float sum = 0;
        for(int i = 0; i < e.getPointerCount(); i++) sum += e.getX(i);
        return sum / e.getPointerCount();
    }

    private static float focusY(MotionEvent e) {
        float sum = 0;
        for(int i = 0; i < e.getPointerCount(); i++) sum += e.getY(i);
        return sum / e.getPointerCount();
    }

    private void fill(int x, int y) {
        int target = pixels[y * gw + x];
        if(target == color) return;
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});
        while(!stack.isEmpty()) {
            int[] c = stack.pop();
            if(c[0] < 0 || c[1] < 0 || c[0] >= gw || c[1] >= gh) continue;
            if(pixels[c[1] * gw + c[0]] != target) continue;
            pixels[c[1] * gw + c[0]] = color;
            stack.push(new int[]{c[0] + 1, c[1]});
            stack.push(new int[]{c[0] - 1, c[1]});
            stack.push(new int[]{c[0], c[1] + 1});
            stack.push(new int[]{c[0], c[1] - 1});
        }
        invalidate();
    }
}
