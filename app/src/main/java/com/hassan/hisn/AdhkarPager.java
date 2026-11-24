package com.hassan.hisn;

/** Pure counting logic for the one-dhikr-per-screen reader. No Android types, so it can be tested on a plain JVM. */
public class AdhkarPager {
    public static final int COUNTED = 0, ITEM_DONE = 1, CHAPTER_DONE = 2;
    public final int[] need;
    public final int[] done;
    public int cur;

    public AdhkarPager(int[] need) {
        this.need = need.clone();
        this.done = new int[need.length];
        for (int i = 0; i < this.need.length; i++) if (this.need[i] < 1) this.need[i] = 1;
    }

    public int size() { return need.length; }

    public boolean finished() {
        for (int i = 0; i < need.length; i++) if (done[i] < need[i]) return false;
        return need.length > 0;
    }

    /** One tap on the counter of the current dhikr. */
    public int tap() {
        if (done[cur] < need[cur]) done[cur]++;
        if (done[cur] < need[cur]) return COUNTED;
        if (cur + 1 < need.length) return ITEM_DONE;
        return finished() ? CHAPTER_DONE : ITEM_DONE;
    }

    /** Moves to the next dhikr (auto-advance or manual). Returns false at the end. */
    public boolean next() {
        if (cur + 1 < need.length) { cur++; return true; }
        return false;
    }

    public boolean prev() {
        if (cur > 0) { cur--; return true; }
        return false;
    }

    public void reset() { java.util.Arrays.fill(done, 0); cur = 0; }
}
