package com.hassan.hisn;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import org.tensorflow.lite.Interpreter;

/** On-device check that a photo shows a prayer rug (MobileNetV2 quantized, ImageNet class 742 "prayer rug"). Nothing is uploaded. */
public class MatClassifier {
    static final int PRAYER_RUG = 742;

    public static class Result {
        public String best;
        public boolean ok;
        public int score;
    }

    public static Result check(Context context, String path) {
        Result result = new Result();
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 900) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            if (bmp == null) return result;
            int rotate = 0;
            try {
                int o = new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                if (o == 6) rotate = 90; else if (o == 3) rotate = 180; else if (o == 8) rotate = 270;
            } catch (Exception ignored) { }
            if (rotate != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            }
            AssetFileDescriptor fd = context.getAssets().openFd("mobilenet.tflite");
            Interpreter interp = new Interpreter(new FileInputStream(fd.getFileDescriptor()).getChannel()
                    .map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength()));
            int min = Math.min(bmp.getWidth(), bmp.getHeight());
            Bitmap[] inputs = {
                    Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, (bmp.getWidth() - min) / 2, (bmp.getHeight() - min) / 2, min, min), 224, 224, true),
                    Bitmap.createScaledBitmap(bmp, 224, 224, true)};
            int bestScore = 0;
            boolean bestIsTop = false;
            for (Bitmap in : inputs) {
                ByteBuffer buf = ByteBuffer.allocateDirect(224 * 224 * 3).order(ByteOrder.nativeOrder());
                int[] px = new int[224 * 224];
                in.getPixels(px, 0, 224, 0, 0, 224, 224);
                for (int v : px) {
                    buf.put((byte) ((v >> 16) & 255));
                    buf.put((byte) ((v >> 8) & 255));
                    buf.put((byte) (v & 255));
                }
                buf.rewind();
                byte[][] out = new byte[1][1001];
                interp.run(buf, out);
                int rug = out[0][PRAYER_RUG] & 255;
                int topIdx = 0, topVal = 0;
                for (int i = 0; i < 1001; i++) {
                    int v = out[0][i] & 255;
                    if (v > topVal) { topVal = v; topIdx = i; }
                }
                if (rug > bestScore) { bestScore = rug; bestIsTop = topIdx == PRAYER_RUG; }
            }
            interp.close();
            result.score = bestScore;
            result.ok = bestScore >= 200 || (bestIsTop && bestScore >= 175);
        } catch (Throwable t) {
            result.ok = false;
            result.best = String.valueOf(t);
        }
        return result;
    }
}
