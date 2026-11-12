package com.divinedoll.launcher.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/** Shares or opens a file from the instance folders through the launcher file provider. */
public final class ShareFile {
    private ShareFile() {}

    public static void send(Context c, File file, boolean open) {
        Toast.makeText(c, R.string.fork_share_preparing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                File dir = new File(c.getCacheDir(), "bedrock");
                if(!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("cache");
                File copy = new File(dir, file.getName());
                try(FileInputStream in = new FileInputStream(file); FileOutputStream out = new FileOutputStream(copy)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".bedrockfiles", copy);
                String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                String mime = ext == null ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                if(mime == null) mime = "*/*";
                Intent intent = new Intent(open ? Intent.ACTION_VIEW : Intent.ACTION_SEND);
                if(open) intent.setDataAndType(uri, mime);
                else { intent.setType(mime); intent.putExtra(Intent.EXTRA_STREAM, uri); }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = open ? intent : Intent.createChooser(intent, file.getName());
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Tools.runOnUiThread(() -> {
                    try { c.startActivity(chooser); }
                    catch(Exception e) { Toast.makeText(c, c.getString(R.string.fork_op_error, String.valueOf(e.getMessage())), Toast.LENGTH_LONG).show(); }
                });
            } catch(Exception e) {
                Tools.runOnUiThread(() -> Toast.makeText(c, c.getString(R.string.fork_op_error, String.valueOf(e.getMessage())), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
