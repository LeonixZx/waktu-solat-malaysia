package com.example.waktusolatmalaysia.customs;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import androidx.core.content.res.ResourcesCompat;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.Ui;

public final class TasbihDialView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Typeface numbers;
  private int count, goal = 33;

  public TasbihDialView(Context context) {
    super(context);
    numbers = ResourcesCompat.getFont(context, R.font.jakarta_bold);
    setClickable(true);
    setFocusable(true);
  }

  public void setCount(int value, int target) {
    count = value;
    goal = target;
    setContentDescription(com.example.waktusolatmalaysia.ui.I18n.choose(getContext(),
        "Tasbih: " + count + ". Sasaran " + goal + ". Ketik untuk menambah.",
        "Tasbih: " + count + ". Goal " + goal + ". Tap to add."));
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) * .82f;
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Ui.color(getContext(), R.color.surface));
    canvas.drawCircle(cx, cy, r, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(Ui.dp(getContext(), 6));
    paint.setColor(Ui.color(getContext(), R.color.prayer_selected));
    canvas.drawCircle(cx, cy, r - Ui.dp(getContext(), 6), paint);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setColor(Ui.color(getContext(), R.color.colorPrimary));
    float progress = count == 0 ? 0 : ((count - 1) % goal + 1) / (float) goal;
    canvas.drawArc(
        cx - r + Ui.dp(getContext(), 6),
        cy - r + Ui.dp(getContext(), 6),
        cx + r - Ui.dp(getContext(), 6),
        cy + r - Ui.dp(getContext(), 6),
        -90,
        360 * progress,
        false,
        paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setTypeface(numbers);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setColor(Ui.color(getContext(), R.color.text_primary));
    paint.setTextSize(Ui.dp(getContext(), count > 9999 ? 50 : 72));
    float width=paint.measureText(String.valueOf(count));
    if(width>r*1.6f) paint.setTextSize(paint.getTextSize()*r*1.6f/width);
    canvas.drawText(String.valueOf(count), cx, cy + Ui.dp(getContext(), 10), paint);
    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(Ui.dp(getContext(), 13));
    paint.setColor(Ui.color(getContext(), R.color.text_secondary));
    canvas.drawText(com.example.waktusolatmalaysia.ui.I18n.choose(getContext(), "KETIK UNTUK BERZIKIR", "TAP TO COUNT"), cx, cy + Ui.dp(getContext(), 43), paint);
  }
}
