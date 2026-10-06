# ساخت APK مسنجر محلی

این پروژه یک Android WebView native است و به سرور LAN متصل می‌شود.

## پیش‌فرض
`http://192.168.1.9:8080/`

داخل برنامه با دکمه `⚙` می‌توان آدرس سرور را تغییر داد.

## ساخت
با Android Studio باز کنید و `app` را Build کنید.

یا با Gradle نصب‌شده:

```bash
gradle assembleDebug
```

خروجی معمولاً در:
`app/build/outputs/apk/debug/app-debug.apk`

این محیط در زمان ساخت فاقد Android SDK/Build Tools بود، بنابراین APK نهایی در اینجا compile نشده است.
