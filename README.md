# Toaster

轻量 Android Toast 库，兼容普通 Toast、自定义样式 Toast 和 Loading Toast。

## 功能

- 普通 `show / showShort / showLong / delayedShow`
- 子线程调用
- 局部 / 全局样式切换
- `showLoading / dismiss`
- Loading 文案可选
- Loading 样式可全局自定义，也可单次自定义

## 接入

Gradle 7.0 以下：

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Gradle 7.0 及以上：

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
android {
    compileOptions {
        targetCompatibility JavaVersion.VERSION_1_8
        sourceCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'com.github.Builder0615:Toaster:1.0.4'
}
```

## 初始化

```java
public final class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Toaster.init(this);
    }
}
```

## 基础用法

```java
Toaster.show("Saved");
Toaster.showShort("Saved");
Toaster.showLong("Network error");
Toaster.delayedShow("Retrying...", 1500);
Toaster.cancel();
```

## Loading

```java
Toaster.showLoading();
Toaster.showLoading("加载中...");
Toaster.dismiss();
```

单次自定义：

```java
ToastParams params = new ToastParams();
params.loading = true;
params.text = "正在同步";
params.duration = ToastParams.DURATION_INFINITE;
params.style = new BlackLoadingToastStyle();
Toaster.show(params);
```

全局自定义：

```java
Toaster.setLoadingStyle(new BlackLoadingToastStyle() {
    @Override
    protected int getTextColor(Context context) {
        return 0xFF1E293B;
    }
});
```

如果需要完全自定义布局，实现 `ILoadingToastStyle` 即可：

```java
public final class MyLoadingStyle implements ILoadingToastStyle<View> {

    @Override
    public View createView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.toast_loading, null);
    }

    @Override
    public void applyLoading(View view, CharSequence text) {
        TextView messageView = view.findViewById(android.R.id.message);
        if (messageView == null) {
            return;
        }
        if (text == null || text.length() == 0) {
            messageView.setVisibility(View.GONE);
            return;
        }
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(text);
    }
}
```

## API

```java
Toaster.show(CharSequence text);
Toaster.show(int id);
Toaster.show(Object object);

Toaster.showShort(CharSequence text);
Toaster.showLong(CharSequence text);
Toaster.delayedShow(CharSequence text, long delayMillis);

Toaster.showLoading();
Toaster.showLoading(CharSequence text);
Toaster.showLoading(int id);
Toaster.showLoading(Object object);
Toaster.dismiss();

Toaster.show(ToastParams params);
Toaster.cancel();

Toaster.setStyle(IToastStyle<?> style);
Toaster.getStyle();

Toaster.setLoadingStyle(ILoadingToastStyle<?> style);
Toaster.getLoadingStyle();

Toaster.setStrategy(IToastStrategy strategy);
Toaster.getStrategy();

Toaster.setInterceptor(IToastInterceptor interceptor);
Toaster.getInterceptor();
```

## Demo

示例 App 里已经补了：

- 默认 Loading
- 无文案 Loading
- 自定义 Loading 布局
- `dismiss`
- 运行时切换 Loading 样式

常见问题保留在 [HelpDoc-zh.md](HelpDoc-zh.md)。
