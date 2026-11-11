package com.divinedoll.launcher.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.extra.ExtraConstants;
import com.divinedoll.launcher.extra.ExtraCore;
import com.divinedoll.launcher.game.renderer.RendererCache;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.multirt.MultiRTUtils;
import com.divinedoll.launcher.multirt.RTSpinnerAdapter;
import com.divinedoll.launcher.multirt.Runtime;
import com.divinedoll.launcher.instances.InstanceIconProvider;
import com.divinedoll.launcher.utils.profiles.VersionSelectorDialog;
import com.divinedoll.launcher.utils.CropperUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InstanceEditorFragment extends Fragment implements CropperUtils.CropperReceiver {
    public static final String TAG = "InstanceEditorFragment";

    private Instance mInstance;
    private String mSelectedControlLayout;
    private Button mSaveButton, mDeleteButton, mControlSelectButton, mVersionSelectButton;
    private Spinner mDefaultRuntime, mDefaultRenderer;
    private EditText mDefaultName, mDefaultJvmArgument;
    private TextView mDefaultVersion, mDefaultControl;
    private ImageView mInstanceIcon;
    private CheckBox mSharedDataCheckbox;
    private int mRecommendedIconSize;
    private final ActivityResultLauncher<?> mCropperLauncher = CropperUtils.registerCropper(this, this);

    private List<String> mRenderNames;

    public InstanceEditorFragment(){
        super(R.layout.fragment_instance_editor);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Paths, which can be changed
        String value = (String) ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR);
        if(value != null){
            mSelectedControlLayout = value;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindViews(view);

        RendererCache list = RendererCache.getCompatibleRenderers(view.getContext());
        mRenderNames = list.rendererIds;
        List<String> renderList = new ArrayList<>(list.rendererDisplayNames.length + 1);
        renderList.addAll(Arrays.asList(list.rendererDisplayNames));
        renderList.add(view.getContext().getString(R.string.global_default));
        mDefaultRenderer.setAdapter(new ArrayAdapter<>(view.getContext(), R.layout.item_simple_list_1, renderList));

        // Set up behaviors
        mSaveButton.setOnClickListener(v -> {
            InstanceIconProvider.dropIcon(mInstance);
            save();
            Tools.backToMainMenu(requireActivity());
        });

        mDeleteButton.setOnClickListener(v -> {
            DeleteConfirmDialogFragment dialogFragment = new DeleteConfirmDialogFragment();
            dialogFragment.show(getChildFragmentManager(), "delete_dialog_confirm");
        });

        View.OnClickListener controlSelectListener = getControlSelectListener();
        mControlSelectButton.setOnClickListener(controlSelectListener);
        mDefaultControl.setOnClickListener(controlSelectListener);

        // Setup the expendable list behavior
        View.OnClickListener versionSelectListener = getVersionSelectListener();
        mVersionSelectButton.setOnClickListener(versionSelectListener);
        mDefaultVersion.setOnClickListener(versionSelectListener);

        // Set up the icon change click listener
        mInstanceIcon.setOnClickListener(v -> {
            // Fill recommended size on click to ge the most up to date data
            mRecommendedIconSize = Math.max(v.getWidth(), v.getHeight());
            CropperUtils.startCropper(mCropperLauncher);
        });

        mSharedDataCheckbox.setOnCheckedChangeListener((v,checked) ->{
            mInstance.sharedData = checked;
            int text = R.string.instance_shared_data_off;
            if(checked) text = R.string.instance_shared_data_on;
            mSharedDataCheckbox.setText(text);
        });

        Instance selectedInstance = Instances.loadSelectedInstance();
        Context context = view.getContext();
        if(selectedInstance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        }else {
            loadValues(selectedInstance, context);
        }
        styleEditor(view);
    }

    /** Same flat dark look as the rest of the launcher, plus the content hub and the button pack picker. */
    private void styleEditor(View view) {
        Context c = view.getContext();
        view.setBackgroundColor(com.divinedoll.launcher.utils.ThemeColors.background());
        if(com.divinedoll.launcher.utils.UiStyle.rounded()) {
            com.divinedoll.launcher.utils.DivineTheme.roundCorners(mInstanceIcon, 16);
            styleForm(view, c);
        }
        if(view instanceof android.widget.LinearLayout && mInstance != null) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(c);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            int p = com.divinedoll.launcher.utils.ForkUi.dp(c, 12);
            row.setPadding(p, p, p, 0);
            Button hub = com.divinedoll.launcher.utils.ForkUi.compact(com.divinedoll.launcher.utils.ForkUi.button(c, getString(R.string.fork_editor_hub), true));
            hub.setOnClickListener(v -> InstanceHubFragment.open(requireActivity(), String.valueOf(mInstance.name)));
            Button pack = com.divinedoll.launcher.utils.ForkUi.compact(com.divinedoll.launcher.utils.ForkUi.button(c, getString(R.string.fork_editor_pack), false));
            pack.setOnClickListener(v -> choosePack(c));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(com.divinedoll.launcher.utils.ForkUi.dp(c, 8));
            row.addView(hub, lp);
            row.addView(pack, new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ((android.widget.LinearLayout) view).addView(row, 0);
        }
    }

    private void styleForm(View v, Context c) {
        if(v instanceof Button) {
            boolean primary = "primary".equals(v.getTag());
            int base = primary ? com.divinedoll.launcher.utils.PlayColor.get() : 0xFF3A3F47;
            v.setBackground(com.divinedoll.launcher.utils.UiStyle.button(c, base));
            ((Button) v).setTextColor(primary ? com.divinedoll.launcher.utils.DivineTheme.onColor(base) : 0xFFFFFFFF);
        } else if(v instanceof EditText || v instanceof Spinner || (v instanceof TextView && !(v instanceof android.widget.CompoundButton) && v.isClickable() && v.getBackground() != null)) {
            v.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.sidebar(), 0x33FFFFFF, 1));
            int h = com.divinedoll.launcher.utils.ForkUi.dp(c, 12);
            int w = v.getPaddingLeft() > 0 ? v.getPaddingLeft() : h;
            v.setPadding(w, h, w, h);
        } else if(v instanceof CheckBox) {
            ((CheckBox) v).setButtonTintList(android.content.res.ColorStateList.valueOf(com.divinedoll.launcher.utils.AccentColor.get()));
            v.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.sidebar(), 0x33FFFFFF, 1));
            int h = com.divinedoll.launcher.utils.ForkUi.dp(c, 12);
            v.setPadding(h, h, h, h);
        }
        if(v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for(int i = 0; i < g.getChildCount(); i++) styleForm(g.getChildAt(i), c);
        }
    }

    private void choosePack(Context c) {
        String[] files = {"", com.divinedoll.launcher.utils.ControlPacks.NORMAL, com.divinedoll.launcher.utils.ControlPacks.BEDROCK[0],
                com.divinedoll.launcher.utils.ControlPacks.BEDROCK[1], com.divinedoll.launcher.utils.ControlPacks.BEDROCK[2]};
        String[] labels = new String[files.length];
        labels[0] = getString(R.string.fork_pack_global);
        for(int i = 1; i < files.length; i++) labels[i] = getString(com.divinedoll.launcher.utils.ControlPacks.labelOf(files[i]));
        new androidx.appcompat.app.AlertDialog.Builder(c).setTitle(R.string.fork_pack_title).setItems(labels, (d, which) -> {
            try {
                if(which > 0) com.divinedoll.launcher.utils.ControlPacks.ensure(c, files[which]);
                mSelectedControlLayout = files[which];
                mDefaultControl.setText(files[which]);
            } catch(Exception e) {
                Toast.makeText(c, getString(R.string.fork_pack_error, String.valueOf(e.getMessage())), Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private View.OnClickListener getControlSelectListener() {
        return v -> {
            Bundle bundle = new Bundle(3);
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false);
            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH);

            Tools.swapFragment(requireActivity(),
                    FileSelectorFragment.class, FileSelectorFragment.TAG, bundle);
        };
    }

    private View.OnClickListener getVersionSelectListener() {
        return v -> VersionSelectorDialog.open(v.getContext(), false, (id, snapshot)-> mDefaultVersion.setText(id));
    }

    private static String nullToEmpty(String in) {
        if(in == null) return "";
        return in;
    }

    private void loadValues(@NonNull Instance instance, @NonNull Context context){
        mInstance = instance;
        mInstanceIcon.setImageDrawable(
                InstanceIconProvider.fetchIcon(getResources(), instance)
        );

        // Runtime spinner
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        int jvmIndex = -1;
        if(instance.selectedRuntime != null) {
            jvmIndex = runtimes.indexOf(new Runtime(instance.selectedRuntime));
        }
        mDefaultRuntime.setAdapter(new RTSpinnerAdapter(context, runtimes));
        if(jvmIndex == -1) jvmIndex = runtimes.size() - 1;
        mDefaultRuntime.setSelection(jvmIndex);

        // Renderer spinner
        int rendererIndex = mRenderNames.indexOf(instance.getLaunchRenderer());
        if(rendererIndex == -1) {
            rendererIndex = mDefaultRenderer.getAdapter().getCount() - 1;
        }
        mDefaultRenderer.setSelection(rendererIndex);

        mDefaultVersion.setText(instance.versionId);
        mDefaultJvmArgument.setText(nullToEmpty(instance.jvmArgs));
        mDefaultName.setText(nullToEmpty(instance.name));
        mDefaultControl.setText(mSelectedControlLayout == null ? nullToEmpty(instance.controlLayout) : mSelectedControlLayout);
        mSharedDataCheckbox.setChecked(instance.sharedData);
    }

    private void bindViews(@NonNull View view){
        mDefaultControl = view.findViewById(R.id.vprof_editor_ctrl_spinner);
        mDefaultRuntime = view.findViewById(R.id.vprof_editor_spinner_runtime);
        mDefaultRenderer = view.findViewById(R.id.vprof_editor_instance_renderer);
        mDefaultVersion = view.findViewById(R.id.vprof_editor_version_spinner);

        mDefaultName = view.findViewById(R.id.vprof_editor_instance_name);
        mDefaultJvmArgument = view.findViewById(R.id.vprof_editor_jre_args);

        mSaveButton = view.findViewById(R.id.vprof_editor_save_button);
        mDeleteButton = view.findViewById(R.id.vprof_editor_delete_button);
        mControlSelectButton = view.findViewById(R.id.vprof_editor_ctrl_button);
        mVersionSelectButton = view.findViewById(R.id.vprof_editor_version_button);
        mInstanceIcon = view.findViewById(R.id.vprof_editor_instance_icon);
        mSharedDataCheckbox = view.findViewById(R.id.vprof_editor_data_checkbox_container);
    }

    private void save(){
        //First, check for potential issues in the inputs
        mInstance.versionId = mDefaultVersion.getText().toString();
        mInstance.controlLayout = mDefaultControl.getText().toString();
        mInstance.jvmArgs = mDefaultJvmArgument.getText().toString();

        String newName = mDefaultName.getText().toString();
        if(mInstance.controlLayout.isEmpty()) mInstance.controlLayout = null;
        if(mInstance.jvmArgs.isEmpty()) mInstance.jvmArgs = null;

        Runtime selectedRuntime = (Runtime) mDefaultRuntime.getSelectedItem();
        mInstance.selectedRuntime = (selectedRuntime.name.equals("<Default>") || selectedRuntime.versionString == null)
                ? null : selectedRuntime.name;

        if(mDefaultRenderer.getSelectedItemPosition() == mRenderNames.size()) mInstance.renderer = null;
        else mInstance.renderer = mRenderNames.get(mDefaultRenderer.getSelectedItemPosition());

        try {
            if(!newName.isEmpty() && !newName.equals(mInstance.name))
                Instances.renameInstanceDirectory(mInstance, newName);
            mInstance.name = newName;
            mInstance.write();
        }catch (Exception e) {
            Tools.showErrorRemote(e);
        }
    }

    @Override
    public float getAspectRatio() {
        return 1f;
    }

    @Override
    public int getTargetMaxSide() {
        return mRecommendedIconSize;
    }

    @Override
    public void onCropped(Bitmap contentBitmap) {
        mInstanceIcon.setImageBitmap(contentBitmap);
        Log.i("bitmap", "w="+contentBitmap.getWidth() +" h="+contentBitmap.getHeight());
        try {
            mInstance.encodeNewIcon(contentBitmap);
        }catch (IOException e) {
            Tools.showErrorRemote(e);
        }
    }

    @Override
    public void onFailed(Exception exception) {
        Tools.showErrorRemote(exception);
    }
}
