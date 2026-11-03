package com.example.waktusolatmalaysia.widgets;

import android.content.Context;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import com.example.waktusolatmalaysia.ui.Ui;

/** Preserve the host's layout dimensions; only scale the finished, non-interactive preview. */
public final class WidgetPreviewView extends FrameLayout {
  private SizeF size = new SizeF(1, 1);
  private View content;

  public WidgetPreviewView(Context context) {
    super(context);
    setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  }

  public void show(RemoteViews views, SizeF hostSize) {
    size = hostSize;
    removeAllViews();
    content = views.apply(getContext(), this);
    content.setPivotX(0);
    content.setPivotY(0);
    addView(
        content,
        new LayoutParams(
            Ui.dp(getContext(), size.getWidth()), Ui.dp(getContext(), size.getHeight())));
    requestLayout();
  }

  public SizeF hostSize() {
    return size;
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int nativeWidth = Math.max(1, Ui.dp(getContext(), size.getWidth()));
    int nativeHeight = Math.max(1, Ui.dp(getContext(), size.getHeight()));
    int available =
        MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
            ? nativeWidth
            : MeasureSpec.getSize(widthSpec);
    float scale = Math.min(1f, available / (float) nativeWidth);
    if (content != null) {
      content.measure(
          MeasureSpec.makeMeasureSpec(nativeWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(nativeHeight, MeasureSpec.EXACTLY));
      content.setScaleX(scale);
      content.setScaleY(scale);
    }
    setMeasuredDimension(
        resolveSize(available, widthSpec),
        resolveSize((int) Math.ceil(nativeHeight * scale), heightSpec));
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (content == null) return;
    int x =
        Math.max(
            0, (getWidth() - Math.round(content.getMeasuredWidth() * content.getScaleX())) / 2);
    content.layout(x, 0, x + content.getMeasuredWidth(), content.getMeasuredHeight());
  }
}
