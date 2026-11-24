package com.hassan.hisn;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Hadith browser: collections > books > hadith, with the grading given by the source, and search. */
public class HadithActivity extends Activity {
    LinearLayout body;
    int level = 0;       // 0 collections, 1 books, 2 hadith list, 3 detail, 4 search results
    int col = 0, sec = 0;
    JSONObject curHadith; String curColName; int curSec; String curEd;
    List<Object[]> results;

    static final String[][] GRADES = {
            {"Sahih", L.s("صحيح")}, {"Hasan", L.s("حسن")}, {"Daif", L.s("ضعيف")}, {"Hasan Sahih", L.s("حسن صحيح")}, {"Sahih Lighairihi", L.s("صحيح لغيره")},
            {"Hasan Lighairihi", L.s("حسن لغيره")}, {"Isnaad Hasan", L.s("إسناده حسن")}, {"Isnaad Sahih", L.s("إسناده صحيح")}, {"Isnaad Daif", L.s("إسناده ضعيف")},
            {"Mauquf Sahih", L.s("موقوف صحيح")}, {"Mauquf Daif", L.s("موقوف ضعيف")}, {"Mauquf Hasan", L.s("موقوف حسن")}, {"Maqtu Sahih", L.s("مقطوع صحيح")},
            {"Maqtu Daif", L.s("مقطوع ضعيف")}, {"Sahih Muslim", L.s("في صحيح مسلم")}, {"Sahih Bukhari", L.s("في صحيح البخاري")},
            {"Sahih - Agreed Upon", L.s("صحيح متفق عليه")}, {"Sahih - Bukhari And Muslim", L.s("صحيح، في البخاري ومسلم")}, {"Munkar", L.s("منكر")}, {"Shadh", L.s("شاذ")},
            {"Mawdu", L.s("موضوع")}, {"Maudu", L.s("موضوع")}, {"Hasan Gharib", L.s("حسن غريب")}, {"Sahih Gharib", L.s("صحيح غريب")}, {"Daif Jiddan", L.s("ضعيف جدا")}
    };
    static final String[][] GRADERS = {
            {"Al-Albani", L.s("الألباني")}, {"Zubair Ali Zai", L.s("الزبير علي زئي")}, {"Shuaib Al Arnaut", L.s("شعيب الأرنؤوط")}, {"Abu Ghuddah", L.s("أبو غدة")},
            {"Muhammad Muhyi Al-Din Abdul Hamid", L.s("محمد محيي الدين عبد الحميد")}, {"Muhammad Fouad Abd al-Baqi", L.s("محمد فؤاد عبد الباقي")},
            {"Ahmad Muhammad Shakir", L.s("أحمد محمد شاكر")}, {"Bashar Awad Maarouf", L.s("بشار عواد معروف")}, {"Salim al-Hilali", L.s("سليم الهلالي")}
    };

    static String tr(String s, String[][] map) {
        for (String[] m : map) if (m[0].equalsIgnoreCase(s.trim())) return m[1];
        return s;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout[] root = new LinearLayout[1];
        body = Ui.screen(this, L.s("الأحاديث"), root);
        setContentView(root[0]);
        render();
    }

    @Override public void onBackPressed() {
        if (level == 0) { super.onBackPressed(); return; }
        level = level == 3 ? (results != null && backToResults ? 4 : 2) : (level == 4 ? 0 : level - 1);
        render();
    }
    boolean backToResults = false;

    void render() {
        body.removeAllViews();
        if (level == 0) collections();
        else if (level == 1) books();
        else if (level == 2) list();
        else if (level == 3) detail();
        else resultsView();
        ((android.widget.ScrollView) body.getParent()).scrollTo(0, 0);
    }

    TextView note(String s) { return Ui.text(this, s, 13, Ui.MUTED); }

    void collections() {
        body.addView(note(L.s("نصوص الأحاديث من مجموعة بيانات مفتوحة (الأحاديث المفتوحة) تُحمَّل مرة واحدة لكل كتاب وتُحفظ على الهاتف. درجة الحديث تُعرض كما وردت في المصدر ولا نضيف عليها شيئاً. لا يتوفر شرح للأحاديث في هذا المصدر.")));
        final EditText q = new EditText(this);
        q.setHint(L.s("ابحث في الكتب المحمّلة..."));
        q.setInputType(InputType.TYPE_CLASS_TEXT);
        q.setTextSize(17);
        q.setSingleLine(true);
        body.addView(q);
        Button go = Ui.button(this, L.s("بحث"), Ui.GREEN, Color.WHITE);
        go.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doSearch(q.getText().toString()); } });
        body.addView(go);
        for (int i = 0; i < HadithStore.COLS.length; i++) {
            final int ci = i;
            final String ed = HadithStore.COLS[i][0];
            LinearLayout card = Ui.card(this);
            card.addView(Ui.bold(this, HadithStore.COLS[i][1], 19, Ui.GREEN));
            boolean has = HadithStore.has(this, ed);
            JSONObject idx = has ? HadithStore.index(this, ed) : null;
            card.addView(Ui.text(this, has && idx != null ? L.s("محمّل: ") + Ui.ar(idx.optInt("total")) + L.s(" حديث") : L.s("غير محمّل (نحو ") + Ui.ar(HadithStore.COLS[i][2].replace("0.05", L.s("٠٫١"))) + L.s(" ميجابايت)"), 14, Ui.MUTED));
            card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openCollection(ci); } });
            body.addView(card);
        }
        body.addView(note(L.s("المجموعات المتاحة: الكتب الستة (البخاري، مسلم، أبو داود، الترمذي، النسائي، ابن ماجه)، وموطأ مالك، والأربعون النووية، والأربعون القدسية، والأربعون الدهلوية. كتب أخرى (مثل مسند أحمد وصحيح ابن حبان وغيرها) غير متوفرة في هذا المصدر.")));
    }

    void openCollection(final int ci) {
        col = ci;
        final String ed = HadithStore.COLS[ci][0];
        if (HadithStore.has(this, ed)) { level = 1; render(); return; }
        body.removeAllViews();
        body.addView(Ui.bold(this, HadithStore.COLS[ci][1], 20, Ui.GREEN));
        body.addView(Ui.text(this, L.s("جارٍ تحميل الكتاب وتجهيزه... قد يستغرق دقيقة. لا تغلق الشاشة."), 16, Ui.INK));
        final android.content.Context ctx = getApplicationContext();
        new Thread(new Runnable() { public void run() {
            final boolean ok = HadithStore.download(ctx, ed);
            runOnUiThread(new Runnable() { public void run() {
                if (isFinishing()) return;
                if (ok) { level = 1; render(); }
                else { level = 0; render(); android.widget.Toast.makeText(HadithActivity.this, L.s("تعذّر تحميل الكتاب. تأكد من الإنترنت وحاول مرة أخرى."), android.widget.Toast.LENGTH_LONG).show(); }
            } });
        } }).start();
    }

    void books() {
        String ed = HadithStore.COLS[col][0];
        body.addView(Ui.bold(this, HadithStore.COLS[col][1], 20, Ui.GREEN));
        if (HadithStore.sahihain(ed)) body.addView(note(L.s("هذا الكتاب من الصحيحين، ولا تذكر بيانات المصدر درجة لكل حديث.")));
        body.addView(note(L.en() ? L.s("عناوين الكتب (الأبواب الكبرى) كما وردت في المصدر بالإنجليزية مع الاسم العربي المنقول.") : "أسماء الكتب (الأبواب الكبرى) بالعربية للصحيحين، وبالترقيم لبقية الكتب لأن المصدر يقدم عناوينها بالإنجليزية فقط."));
        JSONObject idx = HadithStore.index(this, ed);
        if (idx == null) return;
        JSONObject secs = idx.optJSONObject("secs");
        List<Integer> ids = new ArrayList<>();
        Iterator<String> it = secs.keys();
        while (it.hasNext()) ids.add(Integer.parseInt(it.next()));
        Collections.sort(ids);
        for (final int id : ids) {
            JSONObject s = secs.optJSONObject(String.valueOf(id));
            String n = HadithNames.name(ed, id, s.optString("n"));
            LinearLayout card = Ui.card(this);
            card.addView(Ui.bold(this, n.isEmpty() ? L.s("مقدمة / بدون عنوان") : n, 16, Ui.INK));
            card.addView(Ui.text(this, Ui.ar(s.optInt("c")) + L.s(" حديث"), 13, Ui.MUTED));
            card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sec = id; shown = 100; level = 2; render(); } });
            body.addView(card);
        }
    }

    String gradeLine(JSONObject h) {
        JSONArray g = h.optJSONArray("g");
        if (g == null || g.length() == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.length(); i++) {
            JSONArray x = g.optJSONArray(i);
            if (x == null) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(tr(x.optString(0), GRADERS)).append(": ").append(tr(x.optString(1), GRADES));
        }
        return sb.toString();
    }

    void list() {
        String ed = HadithStore.COLS[col][0];
        JSONObject idx = HadithStore.index(this, ed);
        String n = HadithNames.name(ed, sec, idx.optJSONObject("secs").optJSONObject(String.valueOf(sec)).optString("n"));
        body.addView(Ui.bold(this, HadithStore.COLS[col][1] + "  •  " + (n.isEmpty() ? L.s("مقدمة") : n), 17, Ui.GREEN));
        final JSONArray a = HadithStore.section(this, ed, sec);
        for (int i = 0; i < Math.min(a.length(), shown); i++) {
            final JSONObject h = a.optJSONObject(i);
            if (h == null) continue;
            card(h, HadithStore.COLS[col][1], sec, ed, false);
        }
        if (a.length() > shown) {
            Button more = Ui.button(this, L.s("عرض المزيد (") + Ui.ar(a.length() - shown) + L.s(" حديث متبقٍ)"), Ui.GREEN, Color.WHITE);
            more.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { shown += 100; int y = ((android.widget.ScrollView) body.getParent()).getScrollY(); render(); ((android.widget.ScrollView) body.getParent()).scrollTo(0, y); } });
            body.addView(more);
        }
    }
    int shown = 100;

    void card(final JSONObject h, final String colName, final int s, final String ed, final boolean fromSearch) {
        LinearLayout card = Ui.card(this);
        String t = h.optString("t");
        card.addView(Ui.text(this, L.s("حديث رقم ") + Ui.ar(h.optString("n")) + (fromSearch ? "  •  " + colName : ""), 13, Ui.GOLD));
        TextView tv = Ui.text(this, t.length() > 260 ? t.substring(0, 260) + " ..." : t, 18, Ui.INK);
        tv.setLineSpacing(0, 1.3f);
        card.addView(tv);
        String g = gradeLine(h);
        if (g != null) card.addView(Ui.text(this, L.s("الدرجة: ") + g.split("\n")[0], 13, Ui.GREEN));
        card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { curHadith = h; curColName = colName; curSec = s; curEd = ed; backToResults = fromSearch; level = 3; render(); } });
        body.addView(card);
    }

    void detail() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.bold(this, curColName + L.s(" • حديث رقم ") + Ui.ar(curHadith.optString("n")), 16, Ui.GOLD));
        TextView tv = Ui.text(this, curHadith.optString("t"), 22, Ui.INK);
        tv.setLineSpacing(0, 1.5f);
        tv.setTextIsSelectable(true);
        tv.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        card.addView(tv);
        body.addView(card);
        LinearLayout gc = Ui.card(this);
        gc.addView(Ui.bold(this, L.s("الدرجة"), 16, Ui.GREEN));
        String g = gradeLine(curHadith);
        gc.addView(Ui.text(this, g != null ? g : (HadithStore.sahihain(curEd) ? L.s("من الصحيحين (لا تذكر بيانات المصدر درجة لكل حديث)") : L.s("لا توجد درجة في المصدر لهذا الحديث")), 16, Ui.INK));
        body.addView(gc);
        body.addView(note(L.s("المصدر: ") + curColName + L.s("، حديث رقم ") + Ui.ar(curHadith.optString("n")) + L.s(". النص من مجموعة الأحاديث المفتوحة. لا يتوفر شرح لهذا الحديث في المصدر، فارجع لأهل العلم لشرحه.")));
    }

    void doSearch(String q) {
        results = HadithStore.search(this, q, 60);
        if (results.isEmpty()) { android.widget.Toast.makeText(this, L.s("لا نتائج في الكتب المحمّلة (حمّل الكتب أولاً)"), android.widget.Toast.LENGTH_LONG).show(); return; }
        level = 4; render();
    }

    void resultsView() {
        body.addView(Ui.bold(this, L.s("نتائج البحث (") + Ui.ar(results.size()) + (results.size() >= 60 ? "+" : "") + ")", 17, Ui.GREEN));
        for (Object[] r : results) card((JSONObject) r[3], (String) r[1], (Integer) r[2], (String) r[0], true);
    }
}
