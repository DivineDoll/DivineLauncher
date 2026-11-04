package git.artdeell.mojo.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import git.artdeell.mojo.R;

/**
 * Small pixel art editor. Drawings are saved as PNG in the app's files and can be exported
 * enlarged to Downloads, ready to be used as an image for a control button.
 */
public class PixelArtEditor {
    private static final int[] PALETTE = {
            0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFF39393C,
            0xFFE04646, 0xFFE8802E, 0xFFE8B02E, 0xFF7CD65A,
            0xFF3C8527, 0xFF29B6C5, 0xFF2E8BE6, 0xFF9B59E8,
            0xFFE86AB5, 0xFF8B5A2B, 0xFF5B3A1A, 0xFFF2D6A8};

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "pixelart");
        if(!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** The gray button as a 32x33 grid, every cell can be edited. */
    private static Bitmap buttonTemplate(Context c) {
        Bitmap bmp = BitmapFactory.decodeResource(c.getResources(), R.drawable.pixel_template_button);
        return bmp.copy(Bitmap.Config.ARGB_8888, true);
    }

    private static String cleanName(String raw) {
        String n = raw.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        return n.isEmpty() ? "pixelart" : n;
    }

    public static void show(Context c) {
        Dialog dialog = new Dialog(c, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        int pad = ForkUi.dp(c, 12);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(ThemeColors.background());
        root.setPadding(pad, pad, pad, pad);

        PixelCanvasView canvas = new PixelCanvasView(c);
        root.addView(canvas, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        ScrollView scroll = new ScrollView(c);
        LinearLayout side = new LinearLayout(c);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(pad, 0, 0, 0);
        scroll.addView(side);
        root.addView(scroll, new LinearLayout.LayoutParams(ForkUi.dp(c, 300), ViewGroup.LayoutParams.MATCH_PARENT));

        side.addView(ForkUi.text(c, c.getString(R.string.fork_pixel_title), 16, true));

        LinearLayout tools = ForkUi.row(c);
        int[] toolIds = {PixelCanvasView.PENCIL, PixelCanvasView.ERASER, PixelCanvasView.FILL, PixelCanvasView.PICK};
        int[] toolNames = {R.string.fork_pixel_pencil, R.string.fork_pixel_eraser, R.string.fork_pixel_fill, R.string.fork_pixel_pick};
        Button[] toolButtons = new Button[4];
        for(int i = 0; i < 4; i++) {
            final int id = toolIds[i];
            Button b = ForkUi.button(c, c.getString(toolNames[i]), i == 0);
            b.setTextSize(11);
            b.setPadding(0, 0, 0, 0);
            toolButtons[i] = b;
            b.setOnClickListener(v -> {
                canvas.setTool(id);
                for(int k = 0; k < 4; k++) McStyle.button(toolButtons[k], toolIds[k] == id);
            });
            tools.addView(b, ForkUi.weight(1f));
        }
        side.addView(tools);

        LinearLayout actions = ForkUi.row(c);
        Button undo = ForkUi.button(c, c.getString(R.string.fork_pixel_undo), false);
        Button clear = ForkUi.button(c, c.getString(R.string.fork_pixel_clear), false);
        undo.setTextSize(11);
        clear.setTextSize(11);
        undo.setOnClickListener(v -> canvas.undo());
        clear.setOnClickListener(v -> canvas.clear());
        actions.addView(undo, ForkUi.weight(1f));
        actions.addView(clear, ForkUi.weight(1f));
        side.addView(actions);

        Button[] swatches = new Button[PALETTE.length + 1];
        LinearLayout paletteBox = new LinearLayout(c);
        paletteBox.setOrientation(LinearLayout.VERTICAL);
        paletteBox.setPadding(0, pad, 0, pad);
        LinearLayout rowView = null;
        for(int i = 0; i < PALETTE.length; i++) {
            if(i % 4 == 0) {
                rowView = ForkUi.row(c);
                paletteBox.addView(rowView);
            }
            final int color = PALETTE[i];
            View sw = new View(c);
            sw.setBackground(ForkUi.box(c, color, 0xFFFFFFFF, 1));
            sw.setOnClickListener(v -> canvas.setColor(color));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ForkUi.dp(c, 34), 1f);
            lp.setMargins(ForkUi.dp(c, 2), ForkUi.dp(c, 2), ForkUi.dp(c, 2), ForkUi.dp(c, 2));
            rowView.addView(sw, lp);
        }
        side.addView(paletteBox);

        Button custom = ForkUi.button(c, c.getString(R.string.fork_pixel_custom_color), false);
        custom.setOnClickListener(v -> ColorPicker.show(c, R.string.fork_pixel_custom_color, canvas.getColor(),
                Color.WHITE, canvas::setColor));
        side.addView(custom);
        canvas.setListener(color -> { });

        Button template = ForkUi.button(c, c.getString(R.string.fork_pixel_template), false);
        template.setOnClickListener(v -> {
            String[] names = {c.getString(R.string.fork_pixel_tpl_button), c.getString(R.string.fork_pixel_tpl_blank16),
                    c.getString(R.string.fork_pixel_tpl_blank32), c.getString(R.string.fork_pixel_tpl_blank64)};
            new AlertDialog.Builder(c).setTitle(R.string.fork_pixel_template).setItems(names, (d, which) -> {
                if(which == 0) canvas.load(buttonTemplate(c));
                else {
                    int n = which == 1 ? 16 : which == 2 ? 32 : 64;
                    canvas.setGridSize(n, n);
                }
            }).show();
        });
        side.addView(template);
        canvas.load(buttonTemplate(c));

        EditText name = new EditText(c);
        name.setHint(R.string.fork_pixel_name_hint);
        name.setHintTextColor(0xFF888888);
        name.setTextColor(Color.WHITE);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setSingleLine();
        McStyle.text(name);
        side.addView(name);

        Button save = ForkUi.button(c, c.getString(R.string.fork_pixel_save), true);
        save.setOnClickListener(v -> {
            try {
                File f = new File(dir(c), cleanName(name.getText().toString()) + ".png");
                writePng(canvas.toBitmap(), f);
                Toast.makeText(c, c.getString(R.string.fork_pixel_saved, f.getName()), Toast.LENGTH_SHORT).show();
            } catch(IOException e) {
                Toast.makeText(c, R.string.fork_pixel_error, Toast.LENGTH_LONG).show();
            }
        });
        side.addView(save);

        Button export = ForkUi.button(c, c.getString(R.string.fork_pixel_export), false);
        export.setOnClickListener(v -> {
            try {
                Bitmap src = canvas.toBitmap();
                int scale = Math.max(1, 256 / Math.max(src.getWidth(), src.getHeight()));
                Bitmap big = Bitmap.createScaledBitmap(src, src.getWidth() * scale, src.getHeight() * scale, false);
                File tmp = new File(c.getCacheDir(), cleanName(name.getText().toString()) + "_x" + scale + ".png");
                writePng(big, tmp);
                String file = OnlineSkins.exportToDownloads(c, tmp);
                tmp.delete();
                Toast.makeText(c, c.getString(R.string.fork_pixel_exported, file), Toast.LENGTH_LONG).show();
            } catch(IOException e) {
                Toast.makeText(c, R.string.fork_pixel_error, Toast.LENGTH_LONG).show();
            }
        });
        side.addView(export);

        Button open = ForkUi.button(c, c.getString(R.string.fork_pixel_open), false);
        open.setOnClickListener(v -> {
            File[] files = dir(c).listFiles((d, n) -> n.endsWith(".png"));
            if(files == null || files.length == 0) {
                Toast.makeText(c, R.string.fork_pixel_none, Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[files.length];
            for(int i = 0; i < files.length; i++) names[i] = files[i].getName();
            new AlertDialog.Builder(c).setTitle(R.string.fork_pixel_open).setItems(names, (d, which) -> {
                Bitmap bmp = BitmapFactory.decodeFile(files[which].getAbsolutePath());
                if(bmp == null || bmp.getWidth() > 128 || bmp.getHeight() > 128) {
                    Toast.makeText(c, R.string.fork_pixel_error, Toast.LENGTH_LONG).show();
                    return;
                }
                canvas.load(bmp.copy(Bitmap.Config.ARGB_8888, false));
                name.setText(names[which].replace(".png", ""));
            }).show();
        });
        side.addView(open);

        Button close = ForkUi.button(c, c.getString(android.R.string.ok), false);
        close.setOnClickListener(v -> dialog.dismiss());
        side.addView(close);

        dialog.setContentView(root);
        dialog.show();
    }

    private static void writePng(Bitmap bmp, File f) throws IOException {
        try(FileOutputStream out = new FileOutputStream(f)) {
            if(!bmp.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException("compress");
        }
    }
}
