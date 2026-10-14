# Toaster

Lightweight Android toast library with normal toast, custom toast, and loading toast support.

## Features

- `show / showShort / showLong / delayedShow`
- Safe calls from sub-threads
- Local and global style overrides
- `showLoading / dismiss`
- Optional loading text
- Global or per-call loading style customization

## Setup

Gradle below 7.0:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Gradle 7.0+:

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
    implementation 'com.github.Builder0615:Toaster:1.0.6'
}
```

## Init

```java
public final class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Toaster.init(this);
    }
}
```

## Usage

```java
Toaster.show("Saved");
Toaster.showShort("Saved");
Toaster.showLong("Network error");
Toaster.delayedShow("Retrying...", 1500);
```

## Loading

```java
Toaster.showLoading();
Toaster.showLoading("Loading...");
Toaster.dismiss();
```

Per-call custom loading:

```java
ToastParams params = new ToastParams();
params.loading = true;
params.text = "Syncing";
params.duration = ToastParams.DURATION_INFINITE;
params.style = new BlackLoadingToastStyle();
Toaster.show(params);
```

Global loading style:

```java
Toaster.setLoadingStyle(new BlackLoadingToastStyle());
```

For full custom layouts, implement `ILoadingToastStyle`.

## API

```java
Toaster.show(CharSequence text);
Toaster.showShort(CharSequence text);
Toaster.showLong(CharSequence text);
Toaster.delayedShow(CharSequence text, long delayMillis);

Toaster.showLoading();
Toaster.showLoading(CharSequence text);
Toaster.dismiss();

Toaster.show(ToastParams params);
Toaster.cancel();

Toaster.setStyle(IToastStyle<?> style);
Toaster.setLoadingStyle(ILoadingToastStyle<?> style);
Toaster.setStrategy(IToastStrategy strategy);
Toaster.setInterceptor(IToastInterceptor interceptor);
```

## Demo

The sample app includes:

- Default loading
- Loading without text
- Custom loading layout
- Dismiss
- Runtime loading style switch

More details are kept in [HelpDoc-en.md](HelpDoc-en.md).
