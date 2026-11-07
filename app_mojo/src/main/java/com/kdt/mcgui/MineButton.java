package com.kdt.mcgui;

import android.content.*;
import android.graphics.*;
import android.util.*;

import git.artdeell.mojo.R;

public class MineButton extends androidx.appcompat.widget.AppCompatButton {
	
	public MineButton(Context ctx) {
		this(ctx, null);
	}
	
	public MineButton(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		git.artdeell.mojo.utils.McStyle.button(this, "primary".equals(getTag()));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._13ssp));
	}


    @Override
    public boolean performClick() {
        git.artdeell.mojo.utils.ClickSound.play();
        return super.performClick();
    }
}
