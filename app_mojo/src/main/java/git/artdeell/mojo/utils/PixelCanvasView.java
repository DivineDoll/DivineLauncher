package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;

/** Square pixel grid for drawing small icons, optionally on top of the gray button base. */
public class PixelCanvasView extends View {
    public static final int PENCIL = 0, ERASER = 1, FILL = 2, PICK = 3;
    private static final int MAX_UNDO = 40;

    public interface Listener {
        void colorPicked(int color);
    }

    private int size = 16;
    private int[] pixels = new int[16 * 16];
    private boolean showBase = true;
    private int tool = PENCIL;
    private int color = Color.WHITE;
    private Listener listener;
    private final ArrayDeque<int[]> undo = new ArrayDeque<>();
    private final Paint paint = new Paint();
    private float cell, left, top;

    public PixelCanvasView(Context context) {
        super(context);
        paint.setAntiAlias(false);
    }

    public void setListener(Listener l) { listener = l; }
    public void setTool(int t) { tool = t; }
    public void setColor(int c) { color = c | 0xFF000000; }
    public int getColor() { return color; }
    public int getGridSize() { return size; }
    public boolean hasBase() { return showBase; }

    public void setBase(boolean on) {
        showBase = on;
        invalidate();
    }

    public void setGridSize(int n) {
        size = n;
        pixels = new int[n * n];
        undo.clear();
        invalidate();
    }

    public void clear() {
        pushUndo();
        pixels = new int[size * size];
        invalidate();
    }

    public void undo() {
        if(undo.isEmpty()) return;
        pixels = undo.pop();
        invalidate();
    }

    /** Replaces the drawing with a square bitmap whose side is the current grid size. */
    public void load(Bitmap bmp) {
        setGridSize(bmp.getWidth());
        bmp.getPixels(pixels, 0, size, 0, 0, size, size);
        showBase = false;
        invalidate();
    }

    private void pushUndo() {
        if(undo.size() >= MAX_UNDO) undo.removeLast();
        undo.push(pixels.clone());
    }

    /** Color of the button base at a cell: black outline, light bevel, dark shadow at the bottom. */
    public static int baseColor(int x, int y, int n) {
        boolean edgeX = x == 0 || x == n - 1, edgeY = y == 0 || y == n - 1;
        if(edgeX && edgeY) return 0;
        if(edgeX || edgeY) return 0xFF000000;
        int bevel = n >= 32 ? 2 : 1;
        if(y >= n - 1 - bevel) return 0xFF39393C;
        if(x <= bevel || x >= n - 1 - bevel || y <= bevel) return 0xFF686868;
        return 0xFF808080;
    }

    /** The finished image: the base (if enabled) with the drawing over it. */
    public Bitmap toBitmap() {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                int p = pixels[y * size + x];
                bmp.setPixel(x, y, p != 0 ? p : (showBase ? baseColor(x, y, size) : 0));
            }
        }
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int side = Math.min(getWidth(), getHeight());
        cell = side / (float) size;
        left = (getWidth() - side) / 2f;
        top = (getHeight() - side) / 2f;
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                int p = pixels[y * size + x];
                if(p == 0 && showBase) p = baseColor(x, y, size);
                if(p == 0) p = ((x + y) & 1) == 0 ? 0xFF4A4A4A : 0xFF3C3C3C;
                paint.setColor(p);
                canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell + 0.5f, top + (y + 1) * cell + 0.5f, paint);
            }
        }
        paint.setColor(0x33FFFFFF);
        paint.setStrokeWidth(1f);
        for(int i = 0; i <= size; i++) {
            canvas.drawLine(left + i * cell, top, left + i * cell, top + side, paint);
            canvas.drawLine(left, top + i * cell, left + side, top + i * cell, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int x = (int) ((e.getX() - left) / cell), y = (int) ((e.getY() - top) / cell);
        boolean inside = x >= 0 && y >= 0 && x < size && y < size;
        if(e.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if(!inside) return true;
            if(tool == PICK) {
                int p = pixels[y * size + x];
                if(p == 0 && showBase) p = baseColor(x, y, size);
                if(p != 0) { color = p; if(listener != null) listener.colorPicked(p); }
                return true;
            }
            pushUndo();
            if(tool == FILL) { fill(x, y); return true; }
        }
        if(inside && (tool == PENCIL || tool == ERASER)
                && (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE)) {
            pixels[y * size + x] = tool == PENCIL ? color : 0;
            invalidate();
        }
        return true;
    }

    private void fill(int x, int y) {
        int target = pixels[y * size + x];
        if(target == color) return;
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});
        while(!stack.isEmpty()) {
            int[] c = stack.pop();
            if(c[0] < 0 || c[1] < 0 || c[0] >= size || c[1] >= size) continue;
            if(pixels[c[1] * size + c[0]] != target) continue;
            pixels[c[1] * size + c[0]] = color;
            stack.push(new int[]{c[0] + 1, c[1]});
            stack.push(new int[]{c[0] - 1, c[1]});
            stack.push(new int[]{c[0], c[1] + 1});
            stack.push(new int[]{c[0], c[1] - 1});
        }
        invalidate();
    }
}
