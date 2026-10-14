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

    /** 等待窗口首次布局的最大帧数，超过后直接显示，避免极端情况下吐司一直不显示 */
    private static final int MAX_LAYOUT_WAIT_FRAMES = 10;

    /** 窗口完成首次布局后额外等待的帧数，用于等待系统应用 gravity 对应的最终位置 */
    private static final int WAIT_FRAMES_AFTER_LAYOUT = 1;

    /** 首帧门控结束之后，吐司淡入显示的时长 */
    private static final long FADE_IN_DURATION_MILLIS = 200;

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
        HANDLER.removeCallbacks(mShowRunnable);
        HANDLER.removeCallbacks(mAutoCancelRunnable);
        if (!isShow()) {
            return;
        }
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
            // 不使用系统的窗口进入动画：在窗口状态不稳定时（例如 Activity 切换、从系统页面返回），
            // 新窗口可能会先被放在左上角，而位置修正会被推迟到进入动画结束才生效，
            // 最终表现为吐司先在左上角淡入、再跳到重力位置。
            // 这里改为先以透明状态添加窗口，等布局稳定后再由框架自己执行淡入。
            params.windowAnimations = 0;
            params.setTitle(WINDOW_TITLE);

            // Loading 窗口会获取焦点，需沿用 Activity 的系统栏图标样式，避免状态栏外观被重置。
            if (mToast.isFillScreen() && mToast.getContext() instanceof Activity) {
                Activity activity = (Activity) mToast.getContext();
                params.systemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) != 0) {
                    params.flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
                }
            }

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
                // 先以完全透明的方式添加窗口，等窗口完成首次布局、系统修正到最终位置后再淡入显示
                mWindowView.setAlpha(0f);
                windowManager.addView(mWindowView, params);
                revealWindowViewAfterLayout();
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

    /**
     * 等窗口完成首次布局、系统应用 gravity 位置之后，再把吐司淡入显示。
     *
     * 在窗口状态不稳定时（例如 Activity 切换、从系统页面返回），WindowManager 可能会先把
     * 新添加的窗口放在左上角，之后才修正到 gravity 对应的位置。如果此时窗口已经可见，
     * 用户就会看到吐司先在左上角出现、再跳到屏幕中间；这里先以透明状态添加窗口，
     * 等首帧布局稳定后再显示，保证用户看到的第一个画面就在正确位置上。
     */
    private void revealWindowViewAfterLayout() {
        final View windowView = mWindowView;
        if (windowView == null) {
            return;
        }
        postOnAnimation(windowView, new Runnable() {

            /** 从添加窗口开始一共等待了多少帧 */
            private int mWaitFrames;

            /** 首次布局完成之后又等待了多少帧 */
            private int mFramesAfterLayout;

            @Override
            public void run() {
                // 窗口已经被取消或者已经被替换，不再继续等待
                if (windowView != mWindowView) {
                    return;
                }

                mWaitFrames++;
                // 窗口还没有附着到 WindowManager 时，getWindowToken 会返回空，这里继续等待
                boolean laidOut = windowView.getWindowToken() != null &&
                        windowView.getWidth() > 0 && windowView.getHeight() > 0;
                if (laidOut) {
                    mFramesAfterLayout++;
                }

                // 等待窗口完成首次布局，布局完成后再多等一帧，让系统把 gravity 的最终位置应用上；
                // 极端情况下最多等待 MAX_LAYOUT_WAIT_FRAMES 帧，避免吐司因为等不到布局一直不显示
                if (mWaitFrames < MAX_LAYOUT_WAIT_FRAMES &&
                        (!laidOut || mFramesAfterLayout <= WAIT_FRAMES_AFTER_LAYOUT)) {
                    postOnAnimation(windowView, this);
                    return;
                }

                if (mToast.getAnimationsId() == 0) {
                    // 外部主动关闭了动画，直接显示
                    windowView.setAlpha(1f);
                } else {
                    windowView.animate().alpha(1f).setDuration(FADE_IN_DURATION_MILLIS).start();
                }
            }
        });
    }

    /**
     * {@link View#postOnAnimation(Runnable)} 需要 Android 4.1 及以上版本
     */
    private static void postOnAnimation(View view, Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.postOnAnimation(runnable);
        } else {
            view.postDelayed(runnable, 16);
        }
    }

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
            if (!isShow()) {
                return;
            }

            try {
                WindowManager windowManager = mWindowLifecycle.getWindowManager();
                if (windowManager == null) {
                    return;
                }

                View windowView = mWindowView != null ? mWindowView : mToast.getView();
                windowView.clearAnimation();
                windowView.animate().cancel();
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) windowView.getLayoutParams();
                params.windowAnimations = 0;
                params.alpha = 0;
                try {
                    windowManager.updateViewLayout(windowView, params);
                } finally {
                    // 先让布局更新把透明度和无动画参数交给系统，再移除窗口。
                    // removeViewImmediate 会跳过这次更新，系统仍可能播放旧的退出动画。
                    windowManager.removeView(windowView);
                }

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
