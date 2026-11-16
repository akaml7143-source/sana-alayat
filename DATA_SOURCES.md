# بيانات المصحف ومصادر التجويد

يعتمد تطبيق **سَنَا الآيَات** على ملف القرآن الأصلي الموجود في جذر المستودع:
`quran-uthmani-kitab.txt`. لا يقوم سكربت البناء بتعديل هذا الملف؛ بل ينشئ بيانات عرض منفصلة في مجلد البناء.

## تخطيط صفحات مصحف المدينة

- المصدر: [ouryhamdalaye/madinah-mushaf-json](https://github.com/ouryhamdalaye/madinah-mushaf-json)
- يوفر تخطيط الصفحات الـ604 برواية حفص، مع ترتيب الأسطر والكلمات.
- الترخيص: **Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)**.
- النسخة المثبتة في البناء: `507eada48f3ffe26a600acf5792a18ba86d6851e`.
- ملف البيانات الناتج `quran-pages.json` يتضمن تخطيط الصفحات المشتق من هذا المصدر، ويُتاح وفق CC BY-SA 4.0 مع الإسناد إلى المصدر.

## ألوان أحكام التجويد

- المصدر: [TheAbubakrAbu/Quran-Tajweed-Engine](https://github.com/TheAbubakrAbu/Quran-Tajweed-Engine)
- المؤلف والمصدر الأصلي: Abubakr Elmallah، ومشروع [Al-Islam | Islamic Pillars](https://github.com/TheAbubakrAbu/Al-Islam-iOS).
- يوفر تعليقات التجويد المسبقة وألوان القواعد، برواية حفص عن عاصم.
- الترخيص: MIT؛ راجع [LICENSE](https://github.com/TheAbubakrAbu/Quran-Tajweed-Engine/blob/main/LICENSE).
- النسخة المثبتة في البناء: `edd7afc0551d4bbbb14416a661534f595c97bb67`.

## التحقق من النص

يتحقق سكربت البناء من عدد الآيات والكلمات وترتيبها ومطابقة الحروف بعد تطبيع اختلافات الرسم والعلامات. إذا لم تتطابق البيانات، يتوقف البناء بدلاً من إنشاء ألوان قد تشير إلى مواضع خاطئة.
