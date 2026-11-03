package com.example.waktusolatmalaysia.customs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.waktusolatmalaysia.R;

public final class CompassView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path arrow = new Path();
  private float heading;
  private float bearing = Float.NaN;

  public CompassView(Context context, @Nullable AttributeSet attributes) {
    super(context, attributes);
  }

  public void setDirection(float heading, float bearing) {
    this.heading = heading;
    this.bearing = bearing;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float radius = Math.min(getWidth(), getHeight()) * 0.42f;
    if (radius <= 0) return;
    float density = getResources().getDisplayMetrics().density;
    int primary = ContextCompat.getColor(getContext(), R.color.prayer_text);
    int divider = ContextCompat.getColor(getContext(), R.color.divider);
    canvas.save();
    canvas.translate(getWidth() / 2f, getHeight() / 2f);
    paint.setShader(
        new android.graphics.RadialGradient(
            0,
            0,
            radius,
            new int[] {
              ContextCompat.getColor(getContext(), R.color.surface),
              ContextCompat.getColor(getContext(), R.color.soft_gold)
            },
            null,
            android.graphics.Shader.TileMode.CLAMP));
    paint.setStyle(Paint.Style.FILL);
    canvas.drawCircle(0, 0, radius, paint);
    paint.setShader(null);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2 * density);
    paint.setColor(divider);
    canvas.drawCircle(0, 0, radius, paint);
    paint.setStrokeWidth(density);
    canvas.drawCircle(0, 0, radius * 0.9f, paint);
    canvas.save();
    canvas.rotate(-heading);
    for (int i = 0; i < 60; i++) {
      paint.setColor(i % 5 == 0 ? primary : divider);
      canvas.drawLine(
          0, -radius + 8 * density, 0, -radius + (i % 5 == 0 ? 20 : 13) * density, paint);
      canvas.rotate(6);
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(primary);
    paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
    paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("N", 0, -radius * 0.66f, paint);
    canvas.drawText("E", radius * 0.72f, 5 * density, paint);
    canvas.drawText("S", 0, radius * 0.76f, paint);
    canvas.drawText("W", -radius * 0.72f, 5 * density, paint);
    canvas.restore();
    if (!Float.isNaN(bearing)) {
      canvas.save();
      canvas.rotate(bearing - heading);
      arrow.reset();
      arrow.moveTo(0, -radius * 0.62f);
      arrow.lineTo(-radius * 0.13f, radius * 0.22f);
      arrow.lineTo(0, radius * 0.1f);
      arrow.lineTo(radius * 0.13f, radius * 0.22f);
      arrow.close();
      paint.setColor(primary);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawPath(arrow, paint);
      arrow.reset();
      arrow.moveTo(0, -radius * .62f);
      arrow.lineTo(0, radius * .1f);
      arrow.lineTo(radius * .13f, radius * .22f);
      arrow.close();
      paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
      canvas.drawPath(arrow, paint);
      canvas.restore();
    }
    paint.setColor(primary);
    paint.setStyle(Paint.Style.FILL);
    canvas.drawCircle(0, 0, 5 * density, paint);
    canvas.restore();
  }
}
