package com.example.waktusolatmalaysia.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import com.example.waktusolatmalaysia.R;

/** A wrapping grid with centred icon labels and full-cell touch targets. */
public final class ShortcutGrid extends GridLayout {
  public ShortcutGrid(Context c) {
    super(c);
    setColumnCount(3);
    setUseDefaultMargins(false);
  }

  public void addShortcut(String title, int icon, Runnable action) {
    LinearLayout cell = Ui.column(getContext());
    cell.setGravity(Gravity.CENTER);
    cell.setMinimumHeight(Ui.dp(getContext(), 112));
    cell.setPadding(
        Ui.dp(getContext(), 8),
        Ui.dp(getContext(), 12),
        Ui.dp(getContext(), 8),
        Ui.dp(getContext(), 12));
    cell.setBackground(Ui.background(getContext(), R.color.surface, 12));
    FrameLayout badge = new FrameLayout(getContext());
    badge.setBackground(Ui.background(getContext(), R.color.soft_gold, 16));
    ImageView symbol = Ui.icon(getContext(), icon, 26);
    symbol.setImageTintList(
        android.content.res.ColorStateList.valueOf(Ui.color(getContext(), R.color.prayer_text)));
    badge.addView(
        symbol,
        new FrameLayout.LayoutParams(
            Ui.dp(getContext(), 26), Ui.dp(getContext(), 26), Gravity.CENTER));
    cell.addView(
        badge, new LinearLayout.LayoutParams(Ui.dp(getContext(), 48), Ui.dp(getContext(), 48)));
    Ui.gap(cell, 8);
    TextView label = Ui.text(getContext(), title, 12, false, R.color.text_primary);
    label.setGravity(Gravity.CENTER);
    label.setMinLines(2);
    cell.addView(label);
    cell.setOnClickListener(v -> action.run());
    addView(cell);
    reflow(getColumnCount());
  }

  @Override
  protected void onMeasure(int widthSpec,int heightSpec) {
    // Natural label height changes with width, language and accessibility scaling.
    // Share the largest height across every tile, including different grid rows.
    for(int i=0;i<getChildCount();i++){
      getChildAt(i).getLayoutParams().height=LayoutParams.WRAP_CONTENT;
      getChildAt(i).forceLayout();
    }
    super.onMeasure(widthSpec,heightSpec);
    int height=0;
    for(int i=0;i<getChildCount();i++)height=Math.max(height,getChildAt(i).getMeasuredHeight());
    if(height>0){
      for(int i=0;i<getChildCount();i++){
        getChildAt(i).getLayoutParams().height=height;
        getChildAt(i).forceLayout();
      }
      super.onMeasure(widthSpec,heightSpec);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    int columns =
        Math.max(
            1,
            Math.min(
                3,
                w
                    / Ui.dp(
                        getContext(),
                        getResources().getConfiguration().fontScale > 1.3f ? 160 : 96)));
    if (columns != getColumnCount()) {
      if (columns < getColumnCount()) {
        reflow(columns);
        setColumnCount(columns);
      } else {
        setColumnCount(columns);
        reflow(columns);
      }
    }
  }

  private void reflow(int columns) {
    for (int i = 0; i < getChildCount(); i++) {
      GridLayout.LayoutParams params =
          new GridLayout.LayoutParams(
              GridLayout.spec(i / columns), GridLayout.spec(i % columns, 1f));
      params.width = 0;
      params.height = -2;
      params.setGravity(Gravity.FILL);
      int gap = Ui.dp(getContext(), 4);
      params.setMargins(gap, gap, gap, gap);
      getChildAt(i).setLayoutParams(params);
    }
  }
}
