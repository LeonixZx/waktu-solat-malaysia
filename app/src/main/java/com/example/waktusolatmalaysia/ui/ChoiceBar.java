package com.example.waktusolatmalaysia.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.GridLayout;
import androidx.core.view.ViewCompat;
import com.example.waktusolatmalaysia.R;
import com.google.android.material.button.MaterialButton;
import java.util.function.IntConsumer;

/** Equal, wrapping choices with explicit selected states and native text scaling. */
public final class ChoiceBar extends GridLayout {
  private int selected;
  private int arrangedColumns = -1;
  private final IntConsumer listener;

  public ChoiceBar(Context c, String[] labels, int[] icons, int initial, IntConsumer change) {
    super(c);
    listener = change;
    setColumnCount(labels.length);
    setPadding(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
    setBackground(Ui.background(c, R.color.control_surface, 16));
    for (int i = 0; i < labels.length; i++) {
      final int index = i;
      MaterialButton b = Ui.button(c, labels[i], icons == null ? 0 : icons[i]);
      b.setCheckable(true);
      b.setTextSize(13);
      b.setMinWidth(0);
      b.setMinimumWidth(0);
      b.setPadding(Ui.dp(c, 6), Ui.dp(c, 8), Ui.dp(c, 6), Ui.dp(c, 8));
      b.setIconPadding(Ui.dp(c, 6));
      b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
      b.setBackgroundTintList(colors(c, R.color.control_selected, R.color.control_surface));
      b.setTextColor(colors(c, R.color.on_control_selected, R.color.text_secondary));
      b.setIconTint(colors(c, R.color.on_control_selected, R.color.text_secondary));
      b.setOnClickListener(v -> select(index, true));
      addView(b);
    }
    select(initial, false);
  }

  private ColorStateList colors(Context c, int active, int normal) {
    return new ColorStateList(
        new int[][] {new int[] {android.R.attr.state_checked}, new int[0]},
        new int[] {Ui.color(c, active), Ui.color(c, normal)});
  }

  public int selected() {
    return selected;
  }

  public void select(int value, boolean notify) {
    selected = Math.max(0, Math.min(getChildCount() - 1, value));
    for (int i = 0; i < getChildCount(); i++) {
      MaterialButton b = (MaterialButton) getChildAt(i);
      b.setChecked(i == selected);
      b.setSelected(i == selected);
      ViewCompat.setStateDescription(
          b, i == selected ? I18n.choose(getContext(), "Dipilih", "Selected") : null);
    }
    if (notify && listener != null) listener.accept(selected);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int available = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
    int minimum = 0;
    for (int i = 0; i < getChildCount(); i++) {
      MaterialButton b = (MaterialButton) getChildAt(i);
      int need =
          Math.round(b.getPaint().measureText(b.getText().toString()))
              + b.getPaddingLeft()
              + b.getPaddingRight()
              + Ui.dp(getContext(), 8);
      if (b.getIcon() != null) need += b.getIconSize() + b.getIconPadding();
      minimum = Math.max(minimum, need);
    }
    int columns = Math.max(1, Math.min(getChildCount(), available / Math.max(1, minimum)));
    if (getChildCount() == 4 && columns == 3) columns = 2;
    if (getChildCount() == 3 && columns == 2) columns = 1;
    if (arrangedColumns != columns) {
      arrangedColumns = columns;
      // Clear old cell specs before shrinking the column count.
      for (int i = 0; i < getChildCount(); i++)
        getChildAt(i).setLayoutParams(new GridLayout.LayoutParams());
      setColumnCount(columns);
      for (int i = 0; i < getChildCount(); i++) {
        LayoutParams p =
            new LayoutParams(GridLayout.spec(i / columns, 1f), GridLayout.spec(i % columns, 1f));
        p.width = 0;
        p.height = LayoutParams.WRAP_CONTENT;
        p.setGravity(Gravity.FILL);
        p.setMargins(
            Ui.dp(getContext(), 2),
            Ui.dp(getContext(), 2),
            Ui.dp(getContext(), 2),
            Ui.dp(getContext(), 2));
        getChildAt(i).setLayoutParams(p);
      }
    }
    super.onMeasure(widthSpec, heightSpec);
  }
}
