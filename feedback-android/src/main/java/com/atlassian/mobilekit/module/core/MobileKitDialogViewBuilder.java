package com.atlassian.mobilekit.module.core;


import androidx.appcompat.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.atlassian.mobilekit.module.feedback.R;

public class MobileKitDialogViewBuilder {

    private final LayoutInflater inflater;
    private final ViewGroup container;
    private int titleResId;
    private int msgResId;
    private int posBtnResId;
    private int negBtnResId;

    private View.OnClickListener posClickListener;
    private View.OnClickListener negClickListener;

    public MobileKitDialogViewBuilder(LayoutInflater inflater, ViewGroup container) {
        this.inflater = inflater;
        this.container = container;
    }

    public MobileKitDialogViewBuilder title(int titleResId) {
        this.titleResId = titleResId;
        return this;
    }

    public MobileKitDialogViewBuilder message(int msgResId) {
        this.msgResId = msgResId;
        return this;
    }

    public MobileKitDialogViewBuilder positiveButton(
            int posBtnResId, View.OnClickListener onClickListener) {

        this.posBtnResId = posBtnResId;
        posClickListener = onClickListener;
        return this;
    }

    public MobileKitDialogViewBuilder negativeButton(
            int negBtnResId, View.OnClickListener onClickListener) {

        this.negBtnResId = negBtnResId;
        negClickListener = onClickListener;
        return this;
    }


    public View build() {

        View dialogView = inflater.inflate(R.layout.mk_feedback_dialog_container, container, false);
        FrameLayout frameLayout = (FrameLayout) dialogView.findViewById(R.id.dialog_container);
        inflater.inflate(R.layout.mk_feedback_dialog_content, frameLayout);

        final AppCompatTextView titleView = (AppCompatTextView) dialogView.findViewById(R.id.title);
        if (titleResId == 0) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(titleResId);
        }

        final AppCompatTextView msgView = (AppCompatTextView) dialogView.findViewById(R.id.message);
        if (msgResId == 0) {
            msgView.setVisibility(View.GONE);
        } else {
            msgView.setText(msgResId);
        }

        final Button posBtn = (Button) dialogView.findViewById(R.id.positive_btn);
        if (posBtnResId == 0) {
            posBtn.setVisibility(View.GONE);
        } else {
            posBtn.setText(posBtnResId);
            posBtn.setOnClickListener(posClickListener);
        }

        final Button negBtn = (Button) dialogView.findViewById(R.id.negative_btn);
        if (negBtnResId == 0) {
            negBtn.setVisibility(View.GONE);
        } else {
            negBtn.setText(negBtnResId);
            negBtn.setOnClickListener(negClickListener);
        }

        return dialogView;
    }


}
