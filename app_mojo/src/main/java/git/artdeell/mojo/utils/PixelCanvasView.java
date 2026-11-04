package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
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

    public PixelCanvasView(Context context) {
        super(context);
        paint.setAntiAlias(false);
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
        cell = Math.min(getWidth() / (float) gw, getHeight() / (float) gh);
        left = (getWidth() - cell * gw) / 2f;
        top = (getHeight() - cell * gh) / 2f;
        for(int y = 0; y < gh; y++) {
            for(int x = 0; x < gw; x++) {
                int p = pixels[y * gw + x];
                if(p == 0) p = ((x + y) & 1) == 0 ? 0xFF4A4A4A : 0xFF3C3C3C;
                paint.setColor(p);
                canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell + 0.5f, top + (y + 1) * cell + 0.5f, paint);
            }
        }
        paint.setColor(0x33FFFFFF);
        paint.setStrokeWidth(1f);
        for(int i = 0; i <= gw; i++) canvas.drawLine(left + i * cell, top, left + i * cell, top + gh * cell, paint);
        for(int i = 0; i <= gh; i++) canvas.drawLine(left, top + i * cell, left + gw * cell, top + i * cell, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(cell <= 0) return true;
        int x = (int) Math.floor((e.getX() - left) / cell), y = (int) Math.floor((e.getY() - top) / cell);
        boolean inside = x >= 0 && y >= 0 && x < gw && y < gh;
        if(e.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if(!inside) return true;
            if(tool == PICK) {
                int p = pixels[y * gw + x];
                if(p != 0) { color = p; if(listener != null) listener.colorPicked(p); }
                return true;
            }
            pushUndo();
            if(tool == FILL) { fill(x, y); return true; }
        }
        if(inside && (tool == PENCIL || tool == ERASER)
                && (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE)) {
            pixels[y * gw + x] = tool == PENCIL ? color : 0;
            invalidate();
        }
        return true;
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
