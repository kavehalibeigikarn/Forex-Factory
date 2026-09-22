# FFNews — هشدار خبرهای قرمز و نارنجی فارکس فکتوری

اپ اندروید (Kotlin + Jetpack Compose) که تقویم هفتگی Forex Factory را می‌گیرد،
فقط خبرهای قرمز (High) و نارنجی (Medium) را نشان می‌دهد و قبل از انتشار هشدار می‌دهد.

## ساخت APK با GitHub Actions
1. یک ریپوی جدید در گیت‌هاب بساز و همه فایل‌های این پوشه را push کن (شاخه main).
2. تب Actions → «Build APK» اجرا می‌شود (یا دستی Run workflow).
3. بعد از پایان، از بخش Artifacts فایل `FFNews-debug-apk` را دانلود و نصب کن.

## منبع داده
https://nfs.faireconomy.media/ff_calendar_thisweek.json — فقط هفته جاری.
