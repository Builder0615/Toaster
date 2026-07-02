package com.hjq.toast.style;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hjq.toast.config.ILoadingToastStyle;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/Toaster
 *    time   : 2026/07/02
 *    desc   : 默认黑色 Loading 样式实现
 */
@SuppressWarnings("deprecation")
public class BlackLoadingToastStyle extends BlackToastStyle implements ILoadingToastStyle<View> {

    @Override
    public LinearLayout createView(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(getTextGravity(context));
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int horizontalPadding = getHorizontalPadding(context);
        int verticalPadding = getVerticalPadding(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layout.setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        } else {
            layout.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            layout.setBackground(getBackgroundDrawable(context));
        } else {
            layout.setBackgroundDrawable(getBackgroundDrawable(context));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            layout.setZ(getTranslationZ(context));
        }

        ProgressBar progressBar = new ProgressBar(context);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(progressBar, progressParams);

        TextView textView = new TextView(context);
        textView.setId(android.R.id.message);
        textView.setGravity(getTextGravity(context));
        textView.setTextColor(getTextColor(context));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize(context));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            12, context.getResources().getDisplayMetrics());
        layout.addView(textView, textParams);
        return layout;
    }

    @Override
    public void applyLoading(View view, CharSequence text) {
        TextView messageView = view.findViewById(android.R.id.message);
        if (messageView == null) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            messageView.setText(null);
            messageView.setVisibility(View.GONE);
            return;
        }
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(text);
    }
}
