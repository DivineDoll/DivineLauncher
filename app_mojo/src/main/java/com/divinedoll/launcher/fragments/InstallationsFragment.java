package com.divinedoll.launcher.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;

import com.divinedoll.launcher.MojoApplication;
import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.instances.DisplayInstance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.utils.ForkUi;

/** "Installations" screen: all instances, tap one to select it, with edit and new buttons. */
public class InstallationsFragment extends Fragment {
    public static final String TAG = "InstallationsFragment";

    private LinearLayout mList;
    private Instances mLastInstances;
    private String mFilter = "";
    private TextView mCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(com.divinedoll.launcher.utils.ThemeColors.background());
        root.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 12), ForkUi.dp(c, 16), ForkUi.dp(c, 12));

        root.addView(ForkUi.pageHeader(c, requireActivity(), ForkUi.TAB_INSTALLS));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = ForkUi.row(c);
        android.widget.EditText search = new android.widget.EditText(c);
        search.setHint(R.string.fork_installs_search);
        search.setSingleLine(true);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                mFilter = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                if(mLastInstances != null) show(mLastInstances);
            }
        });
        header.addView(search, ForkUi.weight(1f));
        Button create = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_installs_new), true));
        create.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                ProfileTypeSelectFragment.TAG, null));
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        createParams.leftMargin = ForkUi.dp(c, 8);
        header.addView(create, createParams);
        Button importButton = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_import_button), false));
        importButton.setOnClickListener(v -> mImport.launch(new String[]{"application/zip", "application/octet-stream", "*/*"}));
        header.addView(importButton, createParams);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mCount = ForkUi.text(c, "", 12, false);
        mCount.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        mCount.setPadding(0, ForkUi.dp(c, 6), 0, ForkUi.dp(c, 6));
        root.addView(mCount);

        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        scroll.setFillViewport(true);
        scroll.addView(mList, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        MojoApplication.sExecutorService.execute(() -> {
            try {
                Instances instances = Instances.loadDisplay();
                Tools.runOnUiThread(() -> show(instances));
            } catch (IOException e) {
                Tools.runOnUiThread(() -> { if(getContext() != null) Tools.showError(getContext(), e); });
            }
        });
    }

    private void show(Instances instances) {
        Context c = getContext();
        if(c == null || mList == null) return;
        mLastInstances = instances;
        mList.removeAllViews();
        if(mCount != null) mCount.setText(getString(R.string.fork_installs_count, instances.list.size()));
        java.util.List<Integer> order = new java.util.ArrayList<>();
        for(int i = 0; i < instances.list.size(); i++) if(matches(instances.list.get(i)) && isFavorite(instances.list.get(i))) order.add(i);
        for(int i = 0; i < instances.list.size(); i++) if(matches(instances.list.get(i)) && !isFavorite(instances.list.get(i))) order.add(i);
        if(order.isEmpty()) {
            TextView empty = ForkUi.text(c, getString(R.string.fork_installs_empty), 14, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, ForkUi.dp(c, 24), 0, 0);
            mList.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        for(int i : order) {
            DisplayInstance instance = instances.list.get(i);
            boolean selected = i == instances.selectedIndex;

            LinearLayout row = ForkUi.row(c);
            int own = com.divinedoll.launcher.utils.InstanceLook.color(String.valueOf(instance.name));
            int accent = own != 0 ? own : com.divinedoll.launcher.utils.AccentColor.get();
            row.setBackground(selected
                    ? ForkUi.box(c, (accent & 0x00FFFFFF) | 0x22000000, accent, 2)
                    : ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.sidebar(), own != 0 ? (own & 0x00FFFFFF) | 0x88000000 : 0x22FFFFFF, own != 0 ? 2 : 1));
            row.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 10), ForkUi.dp(c, 8), ForkUi.dp(c, 10));

            android.widget.ImageView icon = new android.widget.ImageView(c);
            icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            icon.setImageBitmap(iconOf(c, instance));
            icon.setOnClickListener(v -> chooseIcon(c, instance));
            row.addView(icon, new LinearLayout.LayoutParams(ForkUi.dp(c, 48), ForkUi.dp(c, 48)));

            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(ForkUi.dp(c, 10), 0, 0, 0);
            TextView nameView = ForkUi.text(c, String.valueOf(instance.name), 15, true);
            nameView.setSingleLine(true);
            nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(nameView);
            TextView version = ForkUi.text(c, String.valueOf(instance.versionId), 12, false);
            version.setSingleLine(true);
            version.setEllipsize(android.text.TextUtils.TruncateAt.END);
            version.setTextColor(0xFFBBBBBB);
            int tag = com.divinedoll.launcher.utils.InstanceTools.getTag(String.valueOf(instance.name));
            if(tag > 0) {
                version.setText(instance.versionId + "  \u25CF " + getString(com.divinedoll.launcher.utils.InstanceTools.TAG_LABELS[tag]));
                version.setTextColor(com.divinedoll.launcher.utils.InstanceTools.TAG_COLORS[tag]);
            }
            texts.addView(version);
            TextView stats = ForkUi.text(c, com.divinedoll.launcher.utils.InstanceTools.stats(c, String.valueOf(instance.name)), 11, false);
            stats.setTextColor(0xFF999999);
            texts.addView(stats);
            texts.setMinimumWidth(ForkUi.dp(c, 60));
            row.addView(texts, ForkUi.weight(1f));

            if(selected) {
                TextView mark = ForkUi.text(c, "\u2713", 16, true);
                mark.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
                mark.setGravity(Gravity.CENTER);
                mark.setPadding(0, 0, ForkUi.dp(c, 4), 0);
                row.addView(mark);
            }
            TextView star = ForkUi.text(c, isFavorite(instance) ? "\u2605" : "\u2606", 22, false);
            star.setTextColor(0xFFFFD83D);
            star.setPadding(ForkUi.dp(c, 6), 0, ForkUi.dp(c, 6), 0);
            star.setGravity(Gravity.CENTER);
            star.setOnClickListener(v -> {
                com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.edit()
                        .putBoolean(FAVORITE_PREFIX + instance.name, !isFavorite(instance)).apply();
                show(instances);
            });
            row.addView(star);
            Button edit = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_installs_edit), false));
            edit.setOnClickListener(v -> {
                Instances.setSelectedInstance(instance);
                Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
            });
            row.addView(edit);
            Button more = ForkUi.compact(ForkUi.button(c, "\u22EE", false));
            more.setOnClickListener(v -> showMenu(c, instance));
            LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            moreLp.leftMargin = ForkUi.dp(c, 4);
            row.addView(more, moreLp);
            row.setOnLongClickListener(v -> { showMenu(c, instance); return true; });

            row.setOnClickListener(v -> {
                Instances.setSelectedInstance(instance);
                load();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = ForkUi.dp(c, 6);
            mList.addView(row, lp);
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> mImport =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(), uri -> {
                if(uri == null) return;
                Context ctx = requireContext();
                new Thread(() -> {
                    try {
                        com.divinedoll.launcher.instances.Instance imported = com.divinedoll.launcher.utils.InstanceTools.importZip(ctx, uri, getString(R.string.fork_import_suffix));
                        Tools.runOnUiThread(() -> {
                            android.widget.Toast.makeText(ctx, getString(R.string.fork_import_done, String.valueOf(imported.name)), android.widget.Toast.LENGTH_LONG).show();
                            if(isAdded()) load();
                        });
                    } catch (Exception e) {
                        Tools.runOnUiThread(() -> android.widget.Toast.makeText(ctx, getString(R.string.fork_op_error, String.valueOf(e.getMessage())), android.widget.Toast.LENGTH_LONG).show());
                    }
                }).start();
            });

    private com.divinedoll.launcher.instances.Instance mMrpackTarget;
    private final androidx.activity.result.ActivityResultLauncher<String> mMrpackExport =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                com.divinedoll.launcher.instances.Instance inst = mMrpackTarget;
                mMrpackTarget = null;
                if(uri == null || inst == null) return;
                Context ctx = requireContext();
                android.widget.Toast.makeText(ctx, R.string.fork_mrpack_working, android.widget.Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    try {
                        int linked = com.divinedoll.launcher.utils.MrpackExport.export(ctx, inst, uri);
                        Tools.runOnUiThread(() -> android.widget.Toast.makeText(ctx, getString(R.string.fork_mrpack_done, linked), android.widget.Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Tools.runOnUiThread(() -> android.widget.Toast.makeText(ctx, getString(R.string.fork_op_error, String.valueOf(e.getMessage())), android.widget.Toast.LENGTH_LONG).show());
                    }
                }).start();
            });

    private com.divinedoll.launcher.instances.Instance mExporting;
    private final androidx.activity.result.ActivityResultLauncher<String> mExport =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument(), uri -> {
                com.divinedoll.launcher.instances.Instance inst = mExporting;
                mExporting = null;
                if(uri == null || inst == null) return;
                Context ctx = requireContext();
                new Thread(() -> {
                    try {
                        com.divinedoll.launcher.utils.InstanceTools.export(ctx, inst, uri);
                        Tools.runOnUiThread(() -> android.widget.Toast.makeText(ctx, R.string.fork_export_done, android.widget.Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Tools.runOnUiThread(() -> android.widget.Toast.makeText(ctx, getString(R.string.fork_op_error, String.valueOf(e.getMessage())), android.widget.Toast.LENGTH_LONG).show());
                    }
                }).start();
            });

    private void showMenu(Context c, DisplayInstance instance) {
        String[] items = {getString(R.string.fork_menu_files), getString(R.string.fork_menu_duplicate),
                getString(R.string.fork_menu_export), getString(R.string.fork_menu_tag), getString(R.string.fork_menu_mrpack)};
        new androidx.appcompat.app.AlertDialog.Builder(c)
                .setTitle(String.valueOf(instance.name))
                .setItems(items, (d, which) -> {
                    String name = String.valueOf(instance.name);
                    if(which == 3) { chooseTag(c, name); return; }
                    new Thread(() -> {
                        try {
                            com.divinedoll.launcher.instances.Instance full = com.divinedoll.launcher.utils.InstanceTools.find(name);
                            if(which == 0) {
                                Tools.runOnUiThread(() -> { if(isAdded()) InstanceHubFragment.open(requireActivity(), name); });
                            } else if(which == 1) {
                                com.divinedoll.launcher.instances.Instance copy = com.divinedoll.launcher.utils.InstanceTools.duplicate(full, getString(R.string.fork_dup_suffix));
                                Tools.runOnUiThread(() -> {
                                    android.widget.Toast.makeText(c, getString(R.string.fork_dup_done, String.valueOf(copy.name)), android.widget.Toast.LENGTH_LONG).show();
                                    if(isAdded()) load();
                                });
                            } else if(which == 4) {
                                Tools.runOnUiThread(() -> {
                                    mMrpackTarget = full;
                                    mMrpackExport.launch(name.replaceAll("[^A-Za-z0-9._-]", "_") + ".mrpack");
                                });
                            } else {
                                Tools.runOnUiThread(() -> {
                                    mExporting = full;
                                    mExport.launch(name.replaceAll("[^A-Za-z0-9._-]", "_") + ".zip");
                                });
                            }
                        } catch (Exception e) {
                            Tools.runOnUiThread(() -> android.widget.Toast.makeText(c, getString(R.string.fork_op_error, String.valueOf(e.getMessage())), android.widget.Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void chooseTag(Context c, String name) {
        String[] labels = new String[com.divinedoll.launcher.utils.InstanceTools.TAG_LABELS.length];
        for(int i = 0; i < labels.length; i++) labels[i] = getString(com.divinedoll.launcher.utils.InstanceTools.TAG_LABELS[i]);
        new androidx.appcompat.app.AlertDialog.Builder(c)
                .setTitle(R.string.fork_tag_title)
                .setItems(labels, (d, which) -> {
                    com.divinedoll.launcher.utils.InstanceTools.setTag(name, which);
                    if(mLastInstances != null) show(mLastInstances);
                }).show();
    }

    private static final String FAVORITE_PREFIX = "inst_fav_";
    private static final String ICON_PREFIX = "inst_icon_";

    private boolean matches(DisplayInstance instance) {
        if(mFilter.isEmpty()) return true;
        String text = (String.valueOf(instance.name) + " " + instance.versionId).toLowerCase(java.util.Locale.ROOT);
        return text.contains(mFilter);
    }

    private static boolean isFavorite(DisplayInstance instance) {
        return com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.getBoolean(FAVORITE_PREFIX + instance.name, false);
    }

    static android.graphics.Bitmap iconOf(Context c, DisplayInstance instance) {
        String name = com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.getString(ICON_PREFIX + instance.name, null);
        if(name != null) {
            java.io.File file = new java.io.File(com.divinedoll.launcher.utils.PixelArtEditor.directory(c), name);
            android.graphics.Bitmap art = file.isFile() ? android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
            if(art != null) return com.divinedoll.launcher.utils.PixelArtEditor.fitTo(art, 64, 64);
        }
        // The icon the instance already has (the one shown in its editor), before inventing a letter tile
        try {
            android.graphics.Bitmap own = com.divinedoll.launcher.instances.InstanceIconProvider.ownIcon(c.getResources(), instance);
            if(own != null) return android.graphics.Bitmap.createScaledBitmap(own, 128, 128, true);
        } catch(Exception ignored) {}
        return com.divinedoll.launcher.utils.InstanceAvatar.make(String.valueOf(instance.name), 128);
    }

    private DisplayInstance mIconTarget;
    private final androidx.activity.result.ActivityResultLauncher<String> mIconPicker =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                DisplayInstance target = mIconTarget;
                mIconTarget = null;
                if(uri == null || target == null || getContext() == null) return;
                Context ctx = requireContext();
                try (java.io.InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                    android.graphics.Bitmap src = android.graphics.BitmapFactory.decodeStream(in);
                    if(src == null) return;
                    int side = Math.min(src.getWidth(), src.getHeight());
                    android.graphics.Bitmap square = android.graphics.Bitmap.createBitmap(src,
                            (src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
                    android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(square, 128, 128, true);
                    String file = "inst_" + String.valueOf(target.name).replaceAll("[^A-Za-z0-9_-]", "_") + ".png";
                    java.io.File out = new java.io.File(com.divinedoll.launcher.utils.PixelArtEditor.directory(ctx), file);
                    try (java.io.FileOutputStream os = new java.io.FileOutputStream(out)) {
                        small.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
                    }
                    com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.edit().putString(ICON_PREFIX + target.name, file).apply();
                    load();
                } catch (Exception e) {
                    android.widget.Toast.makeText(ctx, getString(R.string.fork_op_error, String.valueOf(e.getMessage())), android.widget.Toast.LENGTH_LONG).show();
                }
            });

    private void chooseIcon(Context c, DisplayInstance instance) {
        java.io.File[] drawings = com.divinedoll.launcher.utils.PixelArtEditor.saved(c);
        String[] names = new String[drawings.length + 2];
        names[0] = getString(R.string.fork_inst_icon_remove);
        names[1] = getString(R.string.fork_inst_icon_gallery);
        for(int i = 0; i < drawings.length; i++) names[i + 2] = drawings[i].getName().replace(".png", "");
        new androidx.appcompat.app.AlertDialog.Builder(c).setTitle(R.string.fork_inst_icon_pick).setItems(names, (d, which) -> {
            if(which == 1) {
                mIconTarget = instance;
                mIconPicker.launch("image/*");
                return;
            }
            android.content.SharedPreferences.Editor editor = com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.edit();
            if(which == 0) editor.remove(ICON_PREFIX + instance.name);
            else editor.putString(ICON_PREFIX + instance.name, drawings[which - 2].getName());
            editor.apply();
            load();
        }).show();
    }
}
