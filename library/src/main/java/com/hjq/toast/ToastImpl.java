package com.hjq.toast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowInsets;
import android.view.ViewGroup;
import android.view.View;
import android.widget.FrameLayout;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/Toaster
 *    time   : 2018/11/02
 *    desc   : 自定义 Toast 实现类
 */
final class ToastImpl {

    private static final String WINDOW_TITLE = "Toast";

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    /** 当前的吐司对象 */
    private final CustomToast mToast;

    /** WindowManager 辅助类 */
    private WindowLifecycle mWindowLifecycle;

    /** 当前应用的包名 */
    private final String mPackageName;

    /** 当前是否已经显示 */
    private boolean mShow;

    /** 当前是否全局显示 */
    private boolean mGlobalShow;

    /** 实际添加到 WindowManager 的 View */
    private View mWindowView;

    private final Runnable mAutoCancelRunnable = new Runnable() {
        @Override
        public void run() {
            cancel();
        }
    };

    ToastImpl(Activity activity, CustomToast toast) {
        this((Context) activity, toast);
        mGlobalShow = false;
        mWindowLifecycle = new WindowLifecycle(activity);
    }

    ToastImpl(Application application, CustomToast toast) {
        this((Context) application, toast);
        mGlobalShow = true;
        mWindowLifecycle = new WindowLifecycle(application);
    }

    private ToastImpl(Context context, CustomToast toast) {
        mToast = toast;
        mPackageName = context.getPackageName();
    }

    boolean isShow() {
        return mShow;
    }

    void setShow(boolean show) {
        mShow = show;
    }

    /***
     * 显示吐司弹窗
     */
    void show() {
        if (isShow()) {
            return;
        }
        if (isMainThread()) {
            mShowRunnable.run();
        } else {
            HANDLER.removeCallbacks(mShowRunnable);
            HANDLER.post(mShowRunnable);
        }
    }

    /**
     * 取消吐司弹窗
     */
    void cancel() {
        if (!isShow()) {
            return;
        }
        HANDLER.removeCallbacks(mShowRunnable);
        HANDLER.removeCallbacks(mAutoCancelRunnable);
        if (isMainThread()) {
            mCancelRunnable.run();
        } else {
            HANDLER.removeCallbacks(mCancelRunnable);
            HANDLER.post(mCancelRunnable);
        }
    }

    /**
     * 判断当前是否在主线程
     */
    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * 发送无障碍事件
     */
    @SuppressWarnings("deprecation")
    private void sendAccessibilityEvent(View view) {
        final Context context = view.getContext();
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        int eventType = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        AccessibilityEvent event;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            event = new AccessibilityEvent();
            event.setEventType(eventType);
        } else {
            event = AccessibilityEvent.obtain(eventType);
        }
        event.setClassName(Toast.class.getName());
        event.setPackageName(context.getPackageName());
        view.dispatchPopulateAccessibilityEvent(event);
        // 将 Toast 视为通知，因为它们用于向用户宣布短暂的信息
        accessibilityManager.sendAccessibilityEvent(event);
    }

    private final Runnable mShowRunnable = new Runnable() {

        @SuppressLint("WrongConstant")
        @Override
        public void run() {
            
            WindowManager windowManager = mWindowLifecycle.getWindowManager();
            if (windowManager == null) {
                return;
            }

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.height = mToast.isFillScreen() ? WindowManager.LayoutParams.MATCH_PARENT : WindowManager.LayoutParams.WRAP_CONTENT;
            params.width = mToast.isFillScreen() ? WindowManager.LayoutParams.MATCH_PARENT : WindowManager.LayoutParams.WRAP_CONTENT;
            params.format = PixelFormat.TRANSLUCENT;
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            if (!mToast.isFillScreen() || !mToast.isInterceptBack()) {
                params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            if (!mToast.isFillScreen()) {
                params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            params.packageName = mPackageName;
            params.gravity = mToast.isFillScreen() ? Gravity.START | Gravity.TOP : mToast.getGravity();
            params.x = mToast.isFillScreen() ? 0 : mToast.getXOffset();
            params.y = mToast.isFillScreen() ? 0 : mToast.getYOffset();
            params.verticalMargin = mToast.isFillScreen() ? 0 : mToast.getVerticalMargin();
            params.horizontalMargin = mToast.isFillScreen() ? 0 : mToast.getHorizontalMargin();
            params.windowAnimations = mToast.getAnimationsId();
            params.setTitle(WINDOW_TITLE);

            // 指定 WindowManager 忽略系统窗口可见性的影响
            // 例如下面这些的显示和隐藏都会影响当前 WindowManager 的显示（触发位置调整）
            // WindowInsets.Type.statusBars()：状态栏
            // WindowInsets.Type.navigationBars()：导航栏
            // WindowInsets.Type.ime()：输入法（软键盘）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Loading 是全屏窗口，但遮罩和内容都应避开系统栏及刘海安全区。
                params.setFitInsetsTypes(WindowInsets.Type.statusBars()
                    | WindowInsets.Type.navigationBars()
                    | WindowInsets.Type.displayCutout());
                params.setFitInsetsIgnoringVisibility(true);
            }

            // 如果是全局显示
            if (mGlobalShow) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    // 在 type 等于 TYPE_APPLICATION_OVERLAY 的时候
                    // 不能添加 WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE 标记
                    // 否则会导致在 Android 13 上面会出现 Toast 布局被半透明化的效果
                    // Github issue 地址：https://github.com/getActivity/Toaster/issues/108
                    params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                } else {
                    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                }
            }

            try {
                mWindowView = createWindowView();
                windowManager.addView(mWindowView, params);
                if (mWindowView.isFocusable()) {
                    mWindowView.requestFocus();
                }
                // 添加一个移除吐司的任务
                HANDLER.removeCallbacks(mAutoCancelRunnable);
                long autoCancelDelay = generateAutoCancelDelay();
                if (autoCancelDelay > 0) {
                    HANDLER.postDelayed(mAutoCancelRunnable, autoCancelDelay);
                }
                // 注册生命周期管控
                mWindowLifecycle.register(ToastImpl.this);
                // 当前已经显示
                setShow(true);
                // 发送无障碍事件
                sendAccessibilityEvent(mToast.getView());
            } catch (Exception e) {
                // 1. 如果这个 View 对象被重复添加到 WindowManager 则会抛出异常
                // java.lang.IllegalStateException: View android.widget.TextView has already been added to the window manager.
                // 2. 如果 WindowManager 绑定的 Activity 已经销毁，则会抛出异常
                // android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@ef1ccb6 is not valid; is your activity running?
                /// 3. 厂商的问题也会导致异常的出现，Github issue 地址：https://github.com/getActivity/Toaster/issues/149
                // java.lang.RuntimeException: InputChannel is not initialized.
                e.printStackTrace();
            }
        }
    };

    private long generateAutoCancelDelay() {
        if (mToast.getTimeoutMillis() > 0) {
            return mToast.getTimeoutMillis();
        }
        if (mToast.getDuration() == Toast.LENGTH_LONG) {
            return mToast.getLongDuration();
        }
        if (mToast.getDuration() == Toast.LENGTH_SHORT) {
            return mToast.getShortDuration();
        }
        return 0;
    }

    private View createWindowView() {
        View contentView = mToast.getView();
        if (!mToast.isFillScreen()) {
            return contentView;
        }

        FrameLayout container = new FrameLayout(mToast.getContext());
        container.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.setBackgroundColor(mToast.isShowMask() ? mToast.getMaskColor() : Color.TRANSPARENT);
        container.setClickable(true);

        if (mToast.isInterceptBack()) {
            container.setFocusable(true);
            container.setFocusableInTouchMode(true);
            container.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            });
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, mToast.getGravity());
        contentView.setTranslationX(mToast.getXOffset());
        contentView.setTranslationY(mToast.getYOffset());
        container.addView(contentView, layoutParams);
        return container;
    }

    private final Runnable mCancelRunnable = new Runnable() {

        @Override
        public void run() {

            try {
                WindowManager windowManager = mWindowLifecycle.getWindowManager();
                if (windowManager == null) {
                    return;
                }

                View windowView = mWindowView != null ? mWindowView : mToast.getView();
                windowManager.removeViewImmediate(windowView);

            } catch (Exception e) {
                // 如果当前 WindowManager 没有添加这个 View 则会抛出异常
                // java.lang.IllegalArgumentException: View=android.widget.TextView not attached to window manager
                e.printStackTrace();
            } finally {
                HANDLER.removeCallbacks(mAutoCancelRunnable);
                // 反注册生命周期管控
                mWindowLifecycle.unregister();
                // 当前没有显示
                setShow(false);
                mWindowView = null;
            }
        }
    };
}
