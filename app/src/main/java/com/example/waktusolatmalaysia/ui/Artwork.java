package com.example.waktusolatmalaysia.ui;

import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.util.concurrent.*;

/** Decode bundled artwork for its measured pixel bounds, away from the main thread. */
public final class Artwork {
  private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final LruCache<String, Bitmap> CACHE =
      new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
          return bitmap.getAllocationByteCount();
        }
      };

  public static void bind(ImageView view, int resource) {
    view.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          if (r - l <= 0 || b - t <= 0) return;
          BitmapFactory.Options bounds = new BitmapFactory.Options();
          bounds.inJustDecodeBounds = true;
          BitmapFactory.decodeResource(view.getResources(), resource, bounds);
          int sample = 1;
          while (bounds.outWidth / (sample * 2) >= r - l
              && bounds.outHeight / (sample * 2) >= b - t) sample *= 2;
          String key = resource + ":" + sample;
          if (key.equals(view.getTag())) return;
          view.setTag(key);
          Bitmap existing = CACHE.get(key);
          if (existing != null) {
            view.setImageBitmap(existing);
            return;
          }
          final int scale = sample;
          WORK.execute(
              () -> {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = scale;
                options.inScaled = false;
                Bitmap bitmap =
                    BitmapFactory.decodeResource(view.getResources(), resource, options);
                if (bitmap == null) return;
                CACHE.put(key, bitmap);
                MAIN.post(
                    () -> {
                      if (key.equals(view.getTag()) && view.isAttachedToWindow())
                        view.setImageBitmap(bitmap);
                    });
              });
        });
  }

  private Artwork() {}
}
