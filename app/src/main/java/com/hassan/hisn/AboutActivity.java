package com.hassan.hisn;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;

public class AboutActivity extends Activity {
    static final String PRIVACY =
            "سياسة الخصوصية\n\n"
            + "تطبيق حصن المسلم مجاني بالكامل، ولا يحتوي على إعلانات ولا حسابات ولا اشتراكات.\n\n"
            + "ما نجمعه: لا نجمع أي بيانات شخصية ولا نرسل أي معلومات عنك إلى أي خادم. الإنترنت يُستخدم فقط لتنزيل محتوى عام يُحفظ على الهاتف بعدها: خطوط صفحات المصحف (verses.quran.foundation) والتفاسير (api.quran.com) من مؤسسة القرآن (Quran Foundation)، ونصوص كتب الأحاديث من cdn.jsdelivr.net. يمكن تحميل المصحف كاملاً مرة واحدة ليعمل بعدها بدون إنترنت، وكل الميزات الأخرى تعمل بدون إنترنت. يرى الخادم عنوان IP العادي كأي تنزيل.\n\n"
            + "الموقع: يُستخدم موقع الهاتف (التقريبي أو الدقيق) داخل الجهاز فقط لحساب مواقيت الصلاة واتجاه القبلة، ولقياس السرعة عند الضغط على زر \"أنا مسافر\" في قفل الصلاة. لا يُرسل ولا يُخزَّن خارج الهاتف.\n\n"
            + "الكاميرا وصور السجادة: عند فتح قفل الصلاة تُلتقط الصورة بتطبيق الكاميرا في هاتفك وتُفحص داخل الجهاز بنموذج ذكاء اصطناعي صغير، ثم تبقى في ذاكرة التطبيق المؤقتة فقط. لا تُرفع الصورة ولا تُشارك.\n\n"
            + "الوصول لاستخدام التطبيقات والظهور فوق التطبيقات: لا تُطلب إلا لميزة قفل الصلاة الاختيارية، ولمعرفة التطبيق الذي أمامك وعرض شاشة القفل فقط. لا تُسجَّل ولا تُرسل أي بيانات عن استخدامك. يمكنك إيقاف الميزة وسحب الأذونات في أي وقت.\n\n"
            + "الظهور فوق التطبيقات: يُستخدم أيضاً لعرض بطاقة الذكر الدورية (اختيارية ويمكن إيقافها من الإعدادات).\n\n"
            + "الإشعارات والمنبهات: لتنبيهك بمواعيد الصلاة والأذكار وتشغيل الأذان.\n\n"
            + "للتواصل بخصوص هذه السياسة: hassanlabib2155@gmail.com";
    static final String CREDITS =
            "المصادر والشكر\n\nبيانات القرآن مقدمة من مؤسسة القرآن (Quran Foundation).\n\n"
            + "• نص القرآن وصفحات المصحف: مصحف المدينة النبوية (طبعة ١٤٠٥هـ) برواية حفص عن عاصم، بخطوط ومواضع الكلمات من مجمع الملك فهد لطباعة المصحف الشريف، عبر واجهة Quran.com (Quran Foundation) المفتوحة. الخطوط تُستخدم دون تعديل.\n\n"
            + "• التفاسير: التفسير الميسر (مجمع الملك فهد)، تفسير السعدي، تفسير ابن كثير، عبر واجهة Quran.com لمؤسسة Quran Foundation.\n\n"
            + "• الأحاديث: نصوص الكتب الستة وموطأ مالك والأربعين من مجموعة hadith-api المفتوحة (github.com/fawazahmed0/hadith-api)، ودرجات الأحاديث كما وردت فيها عن الألباني والزبير علي زئي والأرناؤوط وغيرهم. لا يوجد شرح للأحاديث في هذا المصدر.\n\n"
            + "• الأذكار والأدعية: كتاب حصن المسلم من أذكار الكتاب والسنة لسعيد بن علي بن وهف القحطاني، بصيغة JSON من مشروع asellam/HisnElMuslim (رخصة MIT).\n\n"
            + "• صوت الأذان (Wikimedia Commons):\n"
            + "  - أذان المسجد الحرام، مكة المكرمة، تسجيل Seyfula Islam (CC BY 3.0).\n"
            + "  - أذان المسجد الحرام، المغرب، تسجيل 3omar Faruq (CC BY 3.0).\n"
            + "  - أذان المسجد النبوي، المدينة المنورة، تسجيل ejaz215 (CC BY 3.0).\n"
            + "  - أذان الفجر، مركز مالمو الإسلامي (Islamic Center Malmö) (CC BY 3.0).\n"
            + "  تم قص التسجيلات وضبط مستوى الصوت. رابط الرخصة: creativecommons.org/licenses/by/3.0\n\n"
            + "• التعرف على سجادة الصلاة: نموذج MobileNetV2 من TensorFlow Lite (رخصة Apache 2.0).\n\n"
            + "• حساب المواقيت: خوارزمية فلكية مفتوحة داخل التطبيق، وتمت مقارنتها بنتائج Aladhan API.\n\n"
            + "راجع المختصين في الأذكار قبل الاعتماد الكامل، وتقبل الله منا ومنكم. نسألكم الدعاء.";


    static final String PRIVACY_EN =
            "Privacy policy\n\n"
            + "Hisn al-Muslim is completely free. It has no ads, no accounts and no subscriptions.\n\n"
            + "What we collect: nothing. We do not collect any personal data and we do not send any information about you to any server. The internet is used only to download public content that is then saved on your phone: the Mushaf page fonts (verses.quran.foundation) and the tafsirs (api.quran.com) from the Quran Foundation, and the hadith book texts from cdn.jsdelivr.net. You can download the whole Mushaf once so it works offline, and every other feature works offline. The server sees your normal IP address, as with any download.\n\n"
            + "Location: the phone location (approximate or precise) is used only on the device, to calculate prayer times and the Qibla direction, and to measure speed when you tap \"I'm traveling\" in the prayer lock. It is not sent or stored outside the phone.\n\n"
            + "Camera and prayer mat photos: when you unlock the prayer lock, the photo is taken by your phone's camera app and checked on the device by a small AI model, then kept only in the app's temporary cache. It is never uploaded or shared.\n\n"
            + "Usage access and display over other apps: requested only for the optional prayer lock, to know which app is in front and to show the lock screen. No data about your usage is recorded or sent. You can turn the feature off and withdraw the permissions at any time.\n\n"
            + "Display over other apps is also used to show the periodic dhikr card (optional, and can be turned off in Settings).\n\n"
            + "Notifications and alarms: to alert you of prayer times and adhkar and to play the adhan.\n\n"
            + "To contact us about this policy: hassanlabib2155@gmail.com";
    static final String CREDITS_EN =
            "Sources and thanks\n\nQuran data provided by Quran Foundation.\n\n"
            + "• Quran text and Mushaf pages: the Madinah Mushaf (1405 AH edition) in the narration of Hafs from Asim, with fonts and word positions from the King Fahd Complex for the Printing of the Holy Quran, through the open Quran.com API (Quran Foundation). The fonts are used unmodified.\n\n"
            + "• Tafsirs: At-Tafsir al-Muyassar (King Fahd Complex), Tafsir as-Sa'di and Tafsir Ibn Kathir, through the Quran.com API of the Quran Foundation.\n\n"
            + "• Hadith: the texts of the six books, Muwatta Malik and the Forty collections from the open hadith-api collection (github.com/fawazahmed0/hadith-api), with the hadith grades as given there by al-Albani, Zubair Ali Zai, al-Arna'ut and others. This source has no commentary on the hadith.\n\n"
            + "• Adhkar and supplications: the book Hisn al-Muslim min Adhkar al-Kitab wa al-Sunnah by Sa'id bin Ali bin Wahf al-Qahtani, as JSON from the asellam/HisnElMuslim project (MIT license). English translations and transliterations of some adhkar come from fitrahive/dua-dhikr (MIT license).\n\n"
            + "• Adhan audio (Wikimedia Commons):\n"
            + "  - Adhan of the Sacred Mosque, Makkah, recorded by Seyfula Islam (CC BY 3.0).\n"
            + "  - Adhan of the Sacred Mosque, Maghrib, recorded by 3omar Faruq (CC BY 3.0).\n"
            + "  - Adhan of the Prophet's Mosque, Madinah, recorded by ejaz215 (CC BY 3.0).\n"
            + "  - Fajr adhan, Islamic Center Malmo (CC BY 3.0).\n"
            + "  The recordings were trimmed and volume-adjusted. License: creativecommons.org/licenses/by/3.0\n\n"
            + "• Prayer mat recognition: the MobileNetV2 model from TensorFlow Lite (Apache 2.0 license).\n\n"
            + "• Prayer times: an open astronomical algorithm inside the app, compared with the results of the Aladhan API.\n\n"
            + "Please consult people of knowledge before relying fully on the adhkar. May Allah accept from us and from you. We ask for your dua.";

    static String version(android.content.Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        boolean priv = getIntent().getBooleanExtra("privacy", false);
        boolean en = L.en();
        LinearLayout[] root = new LinearLayout[1];
        LinearLayout body = Ui.screen(this, priv ? L.s("سياسة الخصوصية") : L.s("عن التطبيق"), root);
        LinearLayout card = Ui.card(this);
        String v = version(this);
        String intro = en
                ? "Hisn al-Muslim\nVersion " + v + "\nA free Islamic app, not for profit, a sadaqah jariyah for whoever made it and whoever uses it.\n\n"
                : "حصن المسلم\nالإصدار " + Ui.ar(v) + "\nتطبيق إسلامي مجاني لا يهدف للربح، صدقة جارية على من أعدّه ومن يستخدمه.\n\n";
        String txt = priv ? (en ? PRIVACY_EN : PRIVACY) : intro + (en ? CREDITS_EN : CREDITS);
        TextView t = Ui.text(this, txt, 16, Ui.INK);
        t.setTextIsSelectable(true);
        card.addView(t);
        body.addView(card);
        if (!priv) {
            LinearLayout dev = Ui.card(this);
            dev.addView(Ui.bold(this, L.s("المطوّر"), 17, Ui.GREEN));
            TextView name = Ui.text(this, en ? "Hassan Labib" : "حسن لبيب (Hassan Labib)", 17, Ui.INK);
            name.setPadding(0, Ui.dp(this, 6), 0, 0);
            dev.addView(name);
            TextView mail = Ui.text(this, "hassanlabib2155@gmail.com", 15, Ui.GREEN);
            mail.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            mail.setGravity(en ? Gravity.START : Gravity.END);
            dev.addView(mail);
            Button send = Ui.button(this, L.s("مراسلة المطوّر"), Ui.GREEN, Color.WHITE);
            send.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
                try { startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hassanlabib2155@gmail.com"))); } catch (Exception ignored) { }
            } });
            dev.addView(send);
            body.addView(dev);
        }
        TextView foot = Ui.text(this, en ? "Sadaqah jariyah for Hassan and his parents \u00a9 2026" : "صدقة جارية عن حسن وعن والديه \u00a9 ٢٠٢٦", 14, Ui.MUTED);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 10));
        body.addView(foot);
        setContentView(root[0]);
    }
}
