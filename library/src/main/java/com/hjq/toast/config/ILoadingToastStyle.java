package com.hjq.toast.config;

import android.view.View;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/Toaster
 *    time   : 2026/07/02
 *    desc   : Loading Toast 样式接口
 */
public interface ILoadingToastStyle<V extends View> extends IToastStyle<V> {

    /**
     * 绑定 Loading 文案和状态
     */
    void applyLoading(V view, CharSequence text);
}
