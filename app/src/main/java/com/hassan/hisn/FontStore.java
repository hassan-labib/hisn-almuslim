package com.hassan.hisn;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Page fonts of the mushaf: pages 1-3 are bundled, the rest are downloaded once and cached. */
public class FontStore {
    static final String BASE = "https://verses.quran.foundation/fonts/quran/hafs/v1/ttf/p";

    static File file(Context c, int p) {
        File d = new File(c.getFilesDir(), "fonts");
        if (!d.exists()) d.mkdirs();
        return new File(d, "p" + p + ".ttf");
    }

    static boolean cached(Context c, int p) { return file(c, p).length() > 1000; }

    static boolean bundled(int p) { return p <= 3; }

    static int cachedCount(Context c) {
        int n = 0;
        for (int p = 1; p <= 604; p++) if (bundled(p) || cached(c, p)) n++;
        return n;
    }

    /** Blocking download of one page font. Returns true when the font is available afterwards. */
    static boolean fetch(Context c, int p) {
        if (bundled(p) || cached(c, p)) return true;
        File out = file(c, p), tmp = new File(out.getParentFile(), "p" + p + ".tmp");
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(BASE + p + ".ttf").openConnection();
            h.setConnectTimeout(15000);
            h.setReadTimeout(20000);
            if (h.getResponseCode() != 200) return false;
            InputStream in = h.getInputStream();
            FileOutputStream os = new FileOutputStream(tmp);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            os.close();
            in.close();
            if (tmp.length() < 1000) { tmp.delete(); return false; }
            android.graphics.Typeface.createFromFile(tmp); // throws if it is not a font
            return tmp.renameTo(out);
        } catch (Throwable t) {
            tmp.delete();
            return false;
        } finally {
            if (h != null) h.disconnect();
        }
    }
}
